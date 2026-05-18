package com.libreuml.backend;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Properties;

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

    @MockBean
    protected JavaMailSender javaMailSender;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected void verifyUserEmail(String email) {
        jdbcTemplate.update(
                "UPDATE users SET email_verified_at = NOW() WHERE email = ?", email);
    }

    @BeforeEach
    void setupMailMock() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        org.mockito.Mockito.when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

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
        registry.add("app.mail.from", () -> "no-reply@libreuml.test");
    }
}
