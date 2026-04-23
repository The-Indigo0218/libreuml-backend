package com.libreuml.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreuml.backend.application.emailverification.port.out.EmailSenderPort;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for all integration tests.
 *
 * <p>Uses the Singleton Container pattern: the PostgreSQL container is started exactly
 * once per JVM run via a static initializer.  The JUnit 5 {@code @Container} +
 * {@code @Testcontainers} lifecycle management is deliberately avoided here because it
 * stops the container after each test class finishes.  When multiple test classes share
 * the same static field but the JUnit 5 extension manages the lifecycle independently,
 * Spring Test's ApplicationContext cache reuses the old (dead) datasource URL for
 * subsequent test classes, causing connection timeouts.
 *
 * <p>With this pattern the container lives until JVM exit (Testcontainers' Ryuk cleanup
 * hook handles final removal), and {@link DynamicPropertySource} injects the stable URL
 * into every subclass's ApplicationContext, allowing Spring Test to cache and reuse the
 * same context across all subclasses that share the same configuration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    // No-op EmailSenderPort so registration flows succeed without a real SMTP server.
    // MailSenderAdapter is still wired with the auto-configured JavaMailSender but is never invoked.
    @MockBean
    protected EmailSenderPort emailSenderPort;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    private final Map<String, String> capturedVerificationUrls = new ConcurrentHashMap<>();

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    // Runs before every test (including subclass @BeforeEach, which runs after this).
    // Sets up the emailSenderPort mock to capture verification URLs per email address.
    @BeforeEach
    void captureVerificationEmails() {
        capturedVerificationUrls.clear();
        Mockito.doAnswer(inv -> {
            capturedVerificationUrls.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(emailSenderPort).sendVerificationEmail(Mockito.anyString(), Mockito.anyString());
    }

    /**
     * Completes email verification for a registered user.
     * Extracts the token from the URL captured by the mock and calls the confirm endpoint.
     * Must be called after register() and before login(). Pass the same IP used for registration
     * to avoid hitting the rate limiter on the confirm endpoint.
     */
    protected void verifyEmail(String email, String ip) throws Exception {
        String verificationUrl = capturedVerificationUrls.get(email);
        if (verificationUrl == null) {
            throw new IllegalStateException("No verification URL captured for: " + email);
        }
        String token = verificationUrl.substring(verificationUrl.lastIndexOf("token=") + 6);

        mockMvc.perform(post("/api/v1/auth/email/verify/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}")
                        .header("X-Forwarded-For", ip))
                .andExpect(status().isNoContent());
    }
}
