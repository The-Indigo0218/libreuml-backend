package com.libreuml.backend;

import org.springframework.boot.SpringApplication;

public class TestLibreUmlBackendApplication {

    public static void main(String[] args) {
        SpringApplication.from(LibreUmlBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
