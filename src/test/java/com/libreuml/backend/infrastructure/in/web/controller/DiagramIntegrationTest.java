package com.libreuml.backend.infrastructure.in.web.controller;

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
 * Integration tests for the Diagram CRUD endpoints.
 *
 * <p>Two users are registered per test invocation to exercise ownership-based
 * access control. Real PostgreSQL (Testcontainer) and Flyway migrations run
 * so that JSONB storage, GIN indexes, and the @Version column all behave
 * exactly as they do in production.
 */
class DiagramIntegrationTest extends AbstractIntegrationTest {

    // Offset avoids IP collision with AuthSecurityIntegrationTest (1–4999) and OAuthIntegrationTest (5000+).
    private static final AtomicInteger TEST_ID = new AtomicInteger(9000);

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL    = "/api/v1/auth/login";
    private static final String DIAGRAMS_URL = "/api/v1/diagrams";
    private static final String TEST_PASSWORD = "Test@1234Valid";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private Cookie[] ownerCookies;
    private Cookie[] strangerCookies;
    private String ownerIp;

    @BeforeEach
    void setUp() throws Exception {
        int id = TEST_ID.getAndIncrement();
        ownerIp = "10.88." + (id / 256) + "." + (id % 256);
        String strangerIp = "10.89." + (id / 256) + "." + (id % 256);

        String ownerEmail    = "diagram-owner-"    + id + "@libreuml.io";
        String strangerEmail = "diagram-stranger-" + id + "@libreuml.io";

        register(ownerEmail,    ownerIp);
        register(strangerEmail, strangerIp);

        ownerCookies    = login(ownerEmail,    ownerIp);
        strangerCookies = login(strangerEmail, strangerIp);
    }

    // ---- create ----

    @Test
    void create_withValidPayload_returns201AndDiagramResponse() throws Exception {
        String body = """
                {"title":"Sequence A","type":"SEQUENCE","content":{"nodes":[]}}
                """;

        mockMvc.perform(post(DIAGRAMS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Sequence A"))
                .andExpect(jsonPath("$.type").value("SEQUENCE"))
                .andExpect(jsonPath("$.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void create_withoutAuthentication_returns4xx() throws Exception {
        mockMvc.perform(post(DIAGRAMS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"No Auth","type":"CLASS"}
                                """))
                .andExpect(status().is4xxClientError());
    }

    // ---- 5 MB payload ceiling (enforced in domain aggregate Diagram.create()) ----

    @Test
    void create_withPayloadExceeding5MB_returns422() throws Exception {
        // Construct a JSON object whose serialized form exceeds the 5 MB domain limit.
        // {"data":"aaa..."} where the repeated value pushes the total bytes over 5 * 1024 * 1024.
        String oversizedValue = "a".repeat(5 * 1024 * 1024 + 1);
        String content = objectMapper.writeValueAsString(
                objectMapper.createObjectNode().put("data", oversizedValue));

        String body = "{\"title\":\"Big\",\"type\":\"CLASS\",\"content\":" + content + "}";

        mockMvc.perform(post(DIAGRAMS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Diagram Payload Too Large"));
    }

    // ---- get ----

    @Test
    void get_ownDiagram_returns200() throws Exception {
        String id = createDiagram("Class B", "CLASS");

        mockMvc.perform(get(DIAGRAMS_URL + "/" + id)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Class B"));
    }

    @Test
    void get_anotherUsersDiagram_returns403() throws Exception {
        String id = createDiagram("Private Diagram", "SEQUENCE");

        mockMvc.perform(get(DIAGRAMS_URL + "/" + id)
                        .cookie(strangerCookies))
                .andExpect(status().isForbidden());
    }

    // ---- list ----

    @Test
    void list_returnsOnlyOwnDiagrams() throws Exception {
        createDiagram("My Diagram 1", "CLASS");
        createDiagram("My Diagram 2", "STATE");

        mockMvc.perform(get(DIAGRAMS_URL)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    // ---- update ----

    @Test
    void update_withCorrectVersion_returns200AndIncrementsVersion() throws Exception {
        String id = createDiagram("Original Title", "CLASS");

        String updateBody = """
                {"title":"Updated Title","version":0}
                """;

        mockMvc.perform(patch(DIAGRAMS_URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.version").value(1));
    }

    // ---- 409 Conflict — optimistic locking ----

    @Test
    void update_withStaleVersion_returns409Conflict() throws Exception {
        String id = createDiagram("Concurrency Test", "SEQUENCE");

        // Send a version that does not match the current version (0) — simulates a stale client.
        String staleUpdateBody = """
                {"title":"Stale Update","version":99}
                """;

        mockMvc.perform(patch(DIAGRAMS_URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(staleUpdateBody)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Diagram Conflict"));
    }

    @Test
    void update_afterSuccessfulEdit_withOriginalVersion_returns409() throws Exception {
        String id = createDiagram("Edit Twice", "CLASS");

        // First update: succeeds, advances version from 0 → 1
        mockMvc.perform(patch(DIAGRAMS_URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"First Edit\",\"version\":0}")
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isOk());

        // Second update: client still holds version=0 (stale) → 409
        mockMvc.perform(patch(DIAGRAMS_URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Conflicting Edit\",\"version\":0}")
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isConflict());
    }

    // ---- ownership guard on update/delete ----

    @Test
    void update_byNonOwner_returns403() throws Exception {
        String id = createDiagram("Owner Only", "CLASS");

        mockMvc.perform(patch(DIAGRAMS_URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hijack\",\"version\":0}")
                        .cookie(strangerCookies))
                .andExpect(status().isForbidden());
    }

    // ---- delete ----

    @Test
    void delete_ownDiagram_returns204() throws Exception {
        String id = createDiagram("To Be Deleted", "ACTIVITY");

        mockMvc.perform(delete(DIAGRAMS_URL + "/" + id)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isNoContent());

        // Confirm the diagram is gone
        mockMvc.perform(get(DIAGRAMS_URL + "/" + id)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_byNonOwner_returns403() throws Exception {
        String id = createDiagram("Protected", "SEQUENCE");

        mockMvc.perform(delete(DIAGRAMS_URL + "/" + id)
                        .cookie(strangerCookies))
                .andExpect(status().isForbidden());
    }

    // --- helpers ---

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

    /** Creates a diagram as the owner and returns its UUID string. */
    private String createDiagram(String title, String type) throws Exception {
        String body = String.format("{\"title\":\"%s\",\"type\":\"%s\"}", title, type);
        MvcResult result = mockMvc.perform(post(DIAGRAMS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
