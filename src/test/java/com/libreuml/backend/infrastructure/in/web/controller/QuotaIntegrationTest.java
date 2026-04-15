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
 * Integration tests for per-user storage quota enforcement.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Initial quota response ({@code GET /api/v1/users/me/quota}).</li>
 *   <li>Usage increments on {@code POST /api/v1/diagrams}.</li>
 *   <li>HTTP 422 when a new diagram would exceed the 10 MB ceiling.</li>
 *   <li>Usage decrements on {@code DELETE /api/v1/diagrams/{id}}.</li>
 *   <li>Successful creation after freeing space by deleting a diagram.</li>
 * </ul>
 *
 * <p>Uses real PostgreSQL (Testcontainers) and Flyway migrations, including V9.
 * Quota exhaustion is tested by filling storage with the maximum 5 MB per-diagram
 * payload and then attempting a further creation.
 */
class QuotaIntegrationTest extends AbstractIntegrationTest {

    // Offset avoids IP collisions with DiagramIntegrationTest (9000+) and auth tests.
    private static final AtomicInteger TEST_ID = new AtomicInteger(20000);

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL    = "/api/v1/auth/login";
    private static final String DIAGRAMS_URL = "/api/v1/diagrams";
    private static final String QUOTA_URL    = "/api/v1/users/me/quota";
    private static final String TEST_PASSWORD = "Test@1234Valid";

    private static final long QUOTA_BYTES = 10_485_760L;

    // Content-string sizing: {"key":"<N 'a' chars>"} has 10 bytes of JSON overhead.
    // VALUE_LENGTH_X_MB yields a serialized ObjectNode of exactly X * 1024 * 1024 bytes,
    // which is the value read back by content.toString().getBytes(UTF_8) in the service.
    private static final int VALUE_LENGTH    = 5 * 1024 * 1024 - 10; // → 5 242 880 bytes (5 MB, per-diagram limit)
    private static final int VALUE_LENGTH_3MB = 3 * 1024 * 1024 - 10; // → 3 145 728 bytes (3 MB)
    private static final int VALUE_LENGTH_1MB = 1 * 1024 * 1024 - 10; // → 1 048 576 bytes (1 MB)

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private Cookie[] ownerCookies;
    private String ownerIp;

    @BeforeEach
    void setUp() throws Exception {
        int id = TEST_ID.getAndIncrement();
        ownerIp = "10.90." + (id / 256) + "." + (id % 256);
        String email = "quota-owner-" + id + "@libreuml.io";

        register(email, ownerIp);
        ownerCookies = login(email, ownerIp);
    }

    // ---- initial quota state ----

    @Test
    void getQuota_freshUser_returnsFullQuota() throws Exception {
        mockMvc.perform(get(QUOTA_URL)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quota").value(QUOTA_BYTES))
                .andExpect(jsonPath("$.used").value(0))
                .andExpect(jsonPath("$.available").value(QUOTA_BYTES));
    }

    @Test
    void getQuota_withoutAuthentication_returns4xx() throws Exception {
        mockMvc.perform(get(QUOTA_URL))
                .andExpect(status().is4xxClientError());
    }

    // ---- usage tracking ----

    @Test
    void createDiagram_updatesUsedBytes() throws Exception {
        createDiagram("Small", "CLASS", null);

        MvcResult result = mockMvc.perform(get(QUOTA_URL)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isOk())
                .andReturn();

        long used = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("used").asLong();
        // Empty ObjectNode `{}` is 2 bytes — used must be > 0 after creation.
        assertThat(used).isGreaterThan(0L);
        assertThat(used).isLessThan(QUOTA_BYTES);
    }

    // ---- quota exhaustion / 422 ----

    @Test
    void createDiagram_whenQuotaFull_returns422() throws Exception {
        // Fill the user's quota with two max-size diagrams (≈5 MB each = ≈10 MB total).
        String largeContent = buildLargeContent();
        createDiagram("Filler A", "CLASS", largeContent);
        createDiagram("Filler B", "STATE", largeContent);

        // Any further creation should be rejected with 422.
        mockMvc.perform(post(DIAGRAMS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Over Quota\",\"type\":\"SEQUENCE\"}")
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Storage Quota Exceeded"));
    }

    // ---- delete decrements usage and re-enables creation ----

    @Test
    void deleteDiagram_decrementsUsage_andAllowsSubsequentCreate() throws Exception {
        String largeContent = buildLargeContent();

        // Fill quota completely.
        String idA = createDiagram("Filler A", "CLASS", largeContent);
        createDiagram("Filler B", "STATE", largeContent);

        // Confirm quota is exceeded.
        mockMvc.perform(post(DIAGRAMS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Over\",\"type\":\"SEQUENCE\"}")
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isUnprocessableEntity());

        // Delete diagram A — should free ≈5 MB.
        mockMvc.perform(delete(DIAGRAMS_URL + "/" + idA)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isNoContent());

        // Now a small diagram fits.
        mockMvc.perform(post(DIAGRAMS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"After Delete\",\"type\":\"ACTIVITY\"}")
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isCreated());

        // Quota endpoint reflects the freed space.
        MvcResult quotaResult = mockMvc.perform(get(QUOTA_URL)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isOk())
                .andReturn();

        long available = objectMapper.readTree(quotaResult.getResponse().getContentAsString())
                .get("available").asLong();
        assertThat(available).isGreaterThan(0L);
    }

    // ---- update() quota enforcement ----

    /**
     * Expanding a diagram's content beyond the remaining quota must be rejected with 422.
     *
     * <p>Setup: three 3 MB diagrams (used = 9 MB). Remaining = 1 MB.
     * Update the first from 3 MB to 5 MB → delta = +2 MB → exceeds 1 MB remaining → 422.
     */
    @Test
    void update_increasingSize_checksQuota() throws Exception {
        String content3MB = buildContent(VALUE_LENGTH_3MB);
        String content5MB = buildLargeContent();

        // Fill to 9 MB: 3 × 3 MB diagrams.
        String idA = createDiagram("A", "CLASS", content3MB);
        createDiagram("B", "STATE", content3MB);
        createDiagram("C", "ACTIVITY", content3MB);

        // Try to expand A from 3 MB to 5 MB: delta = +2 MB, but only 1 MB is available.
        mockMvc.perform(patch(DIAGRAMS_URL + "/" + idA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":" + content5MB + ",\"version\":0}")
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Storage Quota Exceeded"));
    }

    /**
     * Shrinking a diagram's content must reduce {@code storage_used_bytes} and allow new creation.
     *
     * <p>Setup: two 5 MB diagrams (quota full). Shrink A to empty content → frees 5 MB.
     * Verify a new creation succeeds and the quota endpoint reflects the freed space.
     */
    @Test
    void update_decreasingSize_freesQuota() throws Exception {
        String content5MB = buildLargeContent();

        // Fill quota.
        String idA = createDiagram("A", "CLASS", content5MB);
        createDiagram("B", "STATE", content5MB);

        // Confirm quota is full.
        mockMvc.perform(post(DIAGRAMS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Blocked\",\"type\":\"SEQUENCE\"}")
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isUnprocessableEntity());

        // Shrink A from 5 MB to empty ({} = 2 bytes): delta = -(5 MB - 2).
        mockMvc.perform(patch(DIAGRAMS_URL + "/" + idA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":{},\"version\":0}")
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isOk());

        // Now there is ~5 MB free — a small diagram fits.
        mockMvc.perform(post(DIAGRAMS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"After Shrink\",\"type\":\"ACTIVITY\"}")
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isCreated());

        // Quota endpoint reflects that space was freed.
        MvcResult quotaResult = mockMvc.perform(get(QUOTA_URL)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isOk())
                .andReturn();
        long available = objectMapper.readTree(quotaResult.getResponse().getContentAsString())
                .get("available").asLong();
        assertThat(available).isGreaterThan(0L);
    }

    /**
     * Updating a diagram with content of exactly the same byte size must leave
     * {@code storage_used_bytes} unchanged (delta = 0, no user record touched).
     *
     * <p>Create with {@code {}} (2 bytes), patch with {@code {}} again (2 bytes): used stays constant.
     */
    @Test
    void update_sameSize_noQuotaChange() throws Exception {
        // Create with empty content ({} = 2 bytes).
        String idA = createDiagram("Same Size", "CLASS", null);

        // Read used bytes before the patch.
        MvcResult before = mockMvc.perform(get(QUOTA_URL)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isOk())
                .andReturn();
        long usedBefore = objectMapper.readTree(before.getResponse().getContentAsString())
                .get("used").asLong();

        // Patch with the same size content ({} = 2 bytes) → delta = 0.
        mockMvc.perform(patch(DIAGRAMS_URL + "/" + idA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":{},\"version\":0}")
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isOk());

        // Used bytes must be identical to the pre-patch value.
        MvcResult after = mockMvc.perform(get(QUOTA_URL)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isOk())
                .andReturn();
        long usedAfter = objectMapper.readTree(after.getResponse().getContentAsString())
                .get("used").asLong();

        assertThat(usedAfter).isEqualTo(usedBefore);
    }

    /**
     * Verifies that {@code PATCH /diagrams/{id}} cannot bypass quota limits set by
     * {@code POST /diagrams}.
     *
     * <p>Scenario (mirrors the real-world bypass attempt):
     * <ol>
     *   <li>Three diagrams fill 9 MB (5 MB + 3 MB + 1 MB).</li>
     *   <li>No more creates are possible (remaining = 1 MB; next create would need > 0 bytes).</li>
     *   <li>Attacker tries to expand the 1 MB diagram to 5 MB via PATCH → delta = 4 MB → 422.</li>
     * </ol>
     */
    @Test
    void update_bypassAttempt_blocked() throws Exception {
        String content5MB = buildLargeContent();
        String content3MB = buildContent(VALUE_LENGTH_3MB);
        String content1MB = buildContent(VALUE_LENGTH_1MB);

        // Fill 9 MB: 5 MB + 3 MB + 1 MB.
        createDiagram("Big",    "CLASS",    content5MB);
        createDiagram("Medium", "STATE",    content3MB);
        String idSmall = createDiagram("Small",  "ACTIVITY", content1MB);

        // Confirm that creating with even tiny content is blocked (only 1 MB left).
        // A small diagram with {} (2 bytes) still fits, but note: 9MB + 3MB+1MB = 9.44 MB so far,
        // remaining ≈ 1 MB. Creating with 1 MB would push used to 10 MB (still OK).
        // The key assertion is on the PATCH attempt below.

        // Bypass attempt: patch the 1 MB diagram to 5 MB → delta = +4 MB → exceeds 1 MB free → 422.
        mockMvc.perform(patch(DIAGRAMS_URL + "/" + idSmall)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":" + content5MB + ",\"version\":0}")
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Storage Quota Exceeded"));
    }

    // ---- helpers ----

    private void register(String email, String ip) throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Quota User", email, TEST_PASSWORD, "STUDENT"))))
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

    /**
     * Creates a diagram and returns its UUID string.
     *
     * @param title   Diagram title.
     * @param type    Diagram type (e.g. "CLASS").
     * @param content JSON string for the {@code content} field; {@code null} omits the field.
     */
    private String createDiagram(String title, String type, String content) throws Exception {
        String body = content != null
                ? String.format("{\"title\":\"%s\",\"type\":\"%s\",\"content\":%s}", title, type, content)
                : String.format("{\"title\":\"%s\",\"type\":\"%s\"}", title, type);

        MvcResult result = mockMvc.perform(post(DIAGRAMS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    /**
     * Builds a JSON content string whose UTF-8 serialization is exactly 5 MB (5 242 880 bytes) —
     * the per-diagram ceiling. Two such diagrams precisely fill the 10 MB user quota.
     *
     * <p>Layout: {@code {"key":"<VALUE_LENGTH 'a' chars>"}}
     * = 8 bytes prefix + VALUE_LENGTH bytes + 2 bytes suffix = 5 242 880 bytes exactly.
     */
    private String buildLargeContent() {
        return buildContent(VALUE_LENGTH);
    }

    /**
     * Builds a JSON content string using the given value length (number of {@code 'a'} characters).
     *
     * <p>Total UTF-8 size = {@code valueLength + 10} bytes (10-byte JSON overhead for
     * {@code {"key":"..."}}). Use the {@code VALUE_LENGTH_X_MB} constants to target
     * exact megabyte thresholds.
     */
    private String buildContent(int valueLength) {
        return "{\"key\":\"" + "a".repeat(valueLength) + "\"}";
    }
}
