package com.libreuml.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class LibreUmlBackendApplicationTests {

    @MockBean
    JavaMailSender javaMailSender;

    @Test
    void contextLoads() {
    }

}
