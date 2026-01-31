package com.libreuml.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class LibreUmlBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibreUmlBackendApplication.class, args);
    }

}
