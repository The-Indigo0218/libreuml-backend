package com.libreuml.backend;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

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
}
