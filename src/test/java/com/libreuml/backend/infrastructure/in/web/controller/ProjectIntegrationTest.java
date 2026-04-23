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

class ProjectIntegrationTest extends AbstractIntegrationTest {

    // Offset avoids collision with AuthSecurityIntegrationTest (1), OAuthIntegrationTest (5000),
    // DiagramIntegrationTest (9000), QuotaIntegrationTest (20000).
    private static final AtomicInteger TEST_ID = new AtomicInteger(30000);

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
        ownerIp = "10.91." + (id / 256) + "." + (id % 256);
        String strangerIp = "10.92." + (id / 256) + "." + (id % 256);

        String ownerEmail    = "proj-owner-"    + id + "@libreuml.io";
        String strangerEmail = "proj-stranger-" + id + "@libreuml.io";

        register(ownerEmail,    ownerIp);
        register(strangerEmail, strangerIp);

        ownerCookies    = login(ownerEmail,    ownerIp);
        strangerCookies = login(strangerEmail, strangerIp);
    }

    // ---- create ----

    @Test
    void createProject_withoutProjectKind_returns201() throws Exception {
        mockMvc.perform(post(PROJECTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"My Project\"}")
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void createProject_withoutProjectKind_defaultsToFree() throws Exception {
        String projectId = createProject("Default Kind Project", null);

        mockMvc.perform(get(PROJECTS_URL + "/" + projectId)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectKind").value("FREE"));
    }

    @Test
    void createProject_withSoftwareArchitectureKind_persistsKind() throws Exception {
        String projectId = createProject("Arch Project", "SOFTWARE_ARCHITECTURE");

        mockMvc.perform(get(PROJECTS_URL + "/" + projectId)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectKind").value("SOFTWARE_ARCHITECTURE"));
    }

    @Test
    void createProject_withFreeKind_persistsKind() throws Exception {
        String projectId = createProject("Free Project", "FREE");

        mockMvc.perform(get(PROJECTS_URL + "/" + projectId)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectKind").value("FREE"));
    }

    @Test
    void createProject_withInvalidProjectKind_returns4xx() throws Exception {
        mockMvc.perform(post(PROJECTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bad Kind\",\"projectKind\":\"INVALID_KIND\"}")
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createProject_withoutAuthentication_returns4xx() throws Exception {
        mockMvc.perform(post(PROJECTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No Auth\"}"))
                .andExpect(status().is4xxClientError());
    }

    // ---- get ----

    @Test
    void getProject_includesProjectKindInResponse() throws Exception {
        String projectId = createProject("With Kind", "SOFTWARE_ARCHITECTURE");

        mockMvc.perform(get(PROJECTS_URL + "/" + projectId)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId))
                .andExpect(jsonPath("$.projectKind").value("SOFTWARE_ARCHITECTURE"));
    }

    @Test
    void getProject_byNonOwner_returns403() throws Exception {
        String projectId = createProject("Private Project", null);

        mockMvc.perform(get(PROJECTS_URL + "/" + projectId)
                        .cookie(strangerCookies))
                .andExpect(status().isForbidden());
    }

    // ---- list ----

    @Test
    void listProjects_includesProjectKindInSummary() throws Exception {
        createProject("Listed SA Project", "SOFTWARE_ARCHITECTURE");

        JsonNode body = objectMapper.readTree(
                mockMvc.perform(get(PROJECTS_URL)
                                .cookie(ownerCookies)
                                .header("X-Forwarded-For", ownerIp))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString()
        );

        boolean found = false;
        for (JsonNode item : body.get("content")) {
            if ("Listed SA Project".equals(item.get("name").asText())) {
                assertThat(item.get("projectKind").asText()).isEqualTo("SOFTWARE_ARCHITECTURE");
                found = true;
            }
        }
        assertThat(found).isTrue();
    }

    // ---- full load ----

    @Test
    void getProjectFull_includesProjectKindInProjectData() throws Exception {
        String projectId = createProject("Full Load Project", "SOFTWARE_ARCHITECTURE");

        mockMvc.perform(get(PROJECTS_URL + "/" + projectId + "/full")
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project.id").value(projectId))
                .andExpect(jsonPath("$.project.projectKind").value("SOFTWARE_ARCHITECTURE"));
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

    /** Creates a project as the owner and returns its UUID string. Pass null for projectKind to omit the field. */
    private String createProject(String name, String projectKind) throws Exception {
        String body = projectKind == null
                ? String.format("{\"name\":\"%s\"}", name)
                : String.format("{\"name\":\"%s\",\"projectKind\":\"%s\"}", name, projectKind);

        MvcResult result = mockMvc.perform(post(PROJECTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
