package com.libreuml.backend.infrastructure.in.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreuml.backend.AbstractIntegrationTest;
import com.libreuml.backend.infrastructure.in.web.dto.request.auth.LoginRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.auth.RegisterRequest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the project-centric API under {@code /api/v1/projects}.
 *
 * <p>Real PostgreSQL (Testcontainer) and Flyway V13 run, so the three tables, the 1:1
 * model constraint, the ON DELETE CASCADE FKs and the @Version columns behave exactly as
 * in production. Two users are registered to exercise ownership-based access control.
 */
class ProjectIntegrationTest extends AbstractIntegrationTest {

    // Offset avoids IP collision with DiagramIntegrationTest (9000+).
    private static final AtomicInteger TEST_ID = new AtomicInteger(11000);

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL    = "/api/v1/auth/login";
    private static final String PROJECTS_URL = "/api/v1/projects";
    private static final String TEST_PASSWORD = "Test@1234Valid";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private Cookie[] ownerCookies;
    private Cookie[] strangerCookies;
    private String ownerIp;

    @BeforeEach
    void setUp() throws Exception {
        int id = TEST_ID.getAndIncrement();
        ownerIp = "10.90." + (id / 256) + "." + (id % 256);
        String strangerIp = "10.91." + (id / 256) + "." + (id % 256);

        String ownerEmail    = "project-owner-"    + id + "@libreuml.io";
        String strangerEmail = "project-stranger-" + id + "@libreuml.io";

        register(ownerEmail,    ownerIp);
        register(strangerEmail, strangerIp);
        verifyUserEmail(ownerEmail);
        verifyUserEmail(strangerEmail);

        ownerCookies    = login(ownerEmail,    ownerIp);
        strangerCookies = login(strangerEmail, strangerIp);
    }

    // ---- create ----

    @Test
    void create_withValidPayload_returns201WithIdAndModelId() throws Exception {
        mockMvc.perform(post(PROJECTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"My Project","projectKind":"FREE","vfsSnapshot":{"root":{}}}
                                """)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.modelId").isNotEmpty())
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void create_withoutAuthentication_returns4xx() throws Exception {
        mockMvc.perform(post(PROJECTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No Auth\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void create_blankName_returns400() throws Exception {
        mockMvc.perform(post(PROJECTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}")
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isBadRequest());
    }

    // ---- model: first write starts at version 0 (empty model created with the project) ----

    @Test
    void updateModel_firstWrite_withVersion0_succeedsAndBumpsTo1() throws Exception {
        String projectId = createProject("Model Project");

        mockMvc.perform(patch(PROJECTS_URL + "/" + projectId + "/model")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":{\"elements\":[]},\"version\":0}")
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void updateModel_staleVersion_returns409() throws Exception {
        String projectId = createProject("Conflict Project");

        // First write moves the model to version 1.
        mockMvc.perform(patch(PROJECTS_URL + "/" + projectId + "/model")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":{},\"version\":0}")
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isOk());

        // Re-sending version 0 must conflict.
        mockMvc.perform(patch(PROJECTS_URL + "/" + projectId + "/model")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":{},\"version\":0}")
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isConflict());
    }

    // ---- diagrams ----

    @Test
    void createDiagram_thenList_returnsTheDiagram() throws Exception {
        String projectId = createProject("Diagram Project");
        createDiagram(projectId, "Domain", "DOMAIN", "vfs-1");

        mockMvc.perform(get(PROJECTS_URL + "/" + projectId + "/diagrams")
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Domain"))
                .andExpect(jsonPath("$[0].diagramType").value("DOMAIN"));
    }

    @Test
    void getDiagram_underWrongProject_returns404() throws Exception {
        String projectA = createProject("Project A");
        String projectB = createProject("Project B");
        String diagramId = createDiagram(projectA, "Class", "CLASS", "vfs-1");

        // diagramId belongs to projectA, not projectB → 404.
        mockMvc.perform(get(PROJECTS_URL + "/" + projectB + "/diagrams/" + diagramId)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isNotFound());
    }

    // ---- ownership ----

    @Test
    void getProject_byStranger_returns403() throws Exception {
        String projectId = createProject("Private Project");

        mockMvc.perform(get(PROJECTS_URL + "/" + projectId)
                        .cookie(strangerCookies))
                .andExpect(status().isForbidden());
    }

    // ---- full + cascade delete ----

    @Test
    void getFull_returnsProjectModelAndDiagrams() throws Exception {
        String projectId = createProject("Full Project");
        createDiagram(projectId, "Seq", "SEQUENCE", "vfs-1");

        mockMvc.perform(get(PROJECTS_URL + "/" + projectId + "/full")
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project.id").value(projectId))
                .andExpect(jsonPath("$.model").isNotEmpty())
                .andExpect(jsonPath("$.diagrams").isArray())
                .andExpect(jsonPath("$.diagrams[0].diagramType").value("SEQUENCE"));
    }

    @Test
    void deleteProject_cascadesModelAndDiagrams() throws Exception {
        String projectId = createProject("Doomed Project");
        createDiagram(projectId, "Class", "CLASS", "vfs-1");

        mockMvc.perform(delete(PROJECTS_URL + "/" + projectId)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isNoContent());

        // The project (and its cascaded model + diagrams) is gone.
        mockMvc.perform(get(PROJECTS_URL + "/" + projectId + "/full")
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_returnsDiagramCountAndTypes() throws Exception {
        String projectId = createProject("Counted Project");
        createDiagram(projectId, "A", "CLASS", "vfs-1");
        createDiagram(projectId, "B", "DOMAIN", "vfs-2");

        JsonNode body = objectMapper.readTree(
                mockMvc.perform(get(PROJECTS_URL + "?size=50")
                                .cookie(ownerCookies)
                                .header("X-Forwarded-For", ownerIp))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());

        JsonNode mine = null;
        for (JsonNode p : body.get("content")) {
            if (p.get("id").asText().equals(projectId)) { mine = p; break; }
        }
        assertThat(mine).isNotNull();
        assertThat(mine.get("diagramCount").asInt()).isEqualTo(2);
        assertThat(mine.get("diagramTypes")).isNotNull();
    }

    // ---- helpers ----

    private void register(String email, String ip) throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Test User", email, TEST_PASSWORD, "STUDENT"))))
                .andExpect(status().isCreated());
    }

    private Cookie[] login(String email, String ip) throws Exception {
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, TEST_PASSWORD))))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie[] cookies = result.getResponse().getCookies();
        assertThat(cookies).isNotEmpty();
        return cookies;
    }

    /** Creates a project as the owner and returns its UUID string. */
    private String createProject(String name) throws Exception {
        String body = String.format(
                "{\"name\":\"%s\",\"projectKind\":\"FREE\",\"vfsSnapshot\":{\"root\":{}}}", name);
        MvcResult result = mockMvc.perform(post(PROJECTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    /** Creates a diagram in the given project and returns its UUID string. */
    private String createDiagram(String projectId, String name, String type, String path) throws Exception {
        String body = String.format(
                "{\"name\":\"%s\",\"diagramType\":\"%s\",\"path\":\"%s\",\"viewData\":{\"nodes\":[]}}",
                name, type, path);
        MvcResult result = mockMvc.perform(post(PROJECTS_URL + "/" + projectId + "/diagrams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
