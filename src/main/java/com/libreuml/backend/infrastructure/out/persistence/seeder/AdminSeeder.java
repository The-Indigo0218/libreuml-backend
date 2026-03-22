package com.libreuml.backend.infrastructure.out.persistence.seeder;

import com.libreuml.backend.application.user.port.out.PasswordEncoderPort;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.Developer;
import com.libreuml.backend.domain.model.RoleEnum;
import com.libreuml.backend.domain.model.User;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @PostConstruct
    public void checkAdminPasswordLength() {
        if (adminPassword.length() < 20) {
            throw new IllegalStateException("Admin password is too short after encoding. Please ensure it is at least 20 characters long.");
        }
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("ℹ️ Seed Admin already exists. Skipping.");
            return;
        }

        log.info("🚀 Seeding Admin User from Environment Variables...");

        User admin = Developer.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .fullName("System Administrator")
                .role(RoleEnum.ADMIN)
                .active(true)
                .joinedAt(LocalDate.now())
                .academicDegrees(java.util.Collections.emptyList())
                .stacks(java.util.Collections.emptyList())
                .organization(java.util.Collections.emptyList())
                .build();

        userRepository.save(admin);
        log.info("✅ Admin created successfully.");
    }
}