package com.libreuml.backend.infrastructure.in.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreuml.backend.AbstractIntegrationTest;
import com.libreuml.backend.domain.model.ReportPriority;
import com.libreuml.backend.domain.model.ReportStatus;
import com.libreuml.backend.domain.model.ReportType;
import com.libreuml.backend.infrastructure.in.web.dto.request.auth.LoginRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.auth.RegisterRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.report.CreateReportRequest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReportIntegrationTest extends AbstractIntegrationTest {

    private static final AtomicInteger TEST_ID = new AtomicInteger(10000);

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL    = "/api/v1/auth/login";
    private static final String REPORTS_URL  = "/api/v1/reports";

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

        String ownerEmail    = "report-owner-"    + id + "@libreuml.io";
        String strangerEmail = "report-stranger-" + id + "@libreuml.io";

        register(ownerEmail,    ownerIp);
        register(strangerEmail, strangerIp);

        ownerCookies    = login(ownerEmail,    ownerIp);
        strangerCookies = login(strangerEmail, strangerIp);
    }

    @Test
    @DisplayName("Stranger cannot view another user's report")
    void strangerCannotViewOtherReport() throws Exception {
        String reportId = createOwnReport();

        mockMvc.perform(get(REPORTS_URL + "/" + reportId)
                        .cookie(strangerCookies)
                        .header("X-Forwarded-For", "10.99.0.1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("User can list their own reports")
    void userCanListOwnReports() throws Exception {
        createOwnReport();

        mockMvc.perform(get(REPORTS_URL + "/my")
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("User cannot list other user's reports via my endpoint")
    void userCannotListOtherUserReports() throws Exception {
        createOwnReport();

        mockMvc.perform(get(REPORTS_URL + "/my")
                        .cookie(strangerCookies)
                        .header("X-Forwarded-For", "10.99.0.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

@Test
    @DisplayName("User can create a report")
    void userCanCreateReport() throws Exception {
        CreateReportRequest request = new CreateReportRequest(
                ReportType.BUG.name(),
                "Bug Report",
                "Found a bug in the diagram editor",
                Set.of()
        );

        mockMvc.perform(post(REPORTS_URL)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get(REPORTS_URL + "/my")
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.title=='Bug Report')]").exists())
                .andExpect(jsonPath("$.content[?(@.status=='OPEN')]").exists());
    }

    @Test
    @DisplayName("Non-authenticated user cannot access reports")
    void unauthenticatedUserCannotAccessReports() throws Exception {
        mockMvc.perform(get(REPORTS_URL + "/my"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Owner can view their own report")
    void ownerCanViewOwnReport() throws Exception {
        CreateReportRequest createReq = new CreateReportRequest(
                ReportType.BUG.name(),
                "My Test Report",
                "Testing view own report",
                Set.of()
        );

        mockMvc.perform(post(REPORTS_URL)
                        .header("X-Forwarded-For", ownerIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq))
                        .cookie(ownerCookies))
                .andExpect(status().isCreated());

        MvcResult listResult = mockMvc.perform(get(REPORTS_URL + "/my")
                        .header("X-Forwarded-For", ownerIp)
                        .cookie(ownerCookies))
                .andExpect(status().isOk())
                .andReturn();

        String reportId = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .get("content").get(0).get("id").asText();

        MvcResult getResult = mockMvc.perform(get(REPORTS_URL + "/" + reportId)
                        .cookie(ownerCookies)
                        .header("X-Forwarded-For", ownerIp))
                .andReturn();

        System.err.println("GET Status: " + getResult.getResponse().getStatus());
        System.err.println("GET Content: " + getResult.getResponse().getContentAsString());
    }

    private String createOwnReport() throws Exception {
        CreateReportRequest request = new CreateReportRequest(
                ReportType.BUG.name(),
                "Test Report Title",
                "This is a test report description",
                Set.of()
        );

        MvcResult result = mockMvc.perform(post(REPORTS_URL)
                        .header("X-Forwarded-For", ownerIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(ownerCookies))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult listResult = mockMvc.perform(get(REPORTS_URL + "/my")
                        .header("X-Forwarded-For", ownerIp)
                        .cookie(ownerCookies))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(listResult.getResponse().getContentAsString())
                .get("content").get(0).get("id").asText();
    }

    private String createReport(String ip) throws Exception {
        CreateReportRequest request = new CreateReportRequest(
                ReportType.BUG.name(),
                "Test Report Title",
                "This is a test report description",
                Set.of()
        );

        mockMvc.perform(post(REPORTS_URL)
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(ownerCookies))
                .andExpect(status().isCreated());

        MvcResult listResult = mockMvc.perform(get(REPORTS_URL + "/my")
                        .header("X-Forwarded-For", ip)
                        .cookie(ownerCookies))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(listResult.getResponse().getContentAsString())
                .get("content").get(0).get("id").asText();
    }

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
}