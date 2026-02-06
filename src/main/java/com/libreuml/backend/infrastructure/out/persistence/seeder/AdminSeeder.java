package com.libreuml.backend.infrastructure.out.persistence.seeder;

import com.libreuml.backend.application.user.port.out.PasswordEncoderPort;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.Developer;
import com.libreuml.backend.domain.model.RoleEnum;
import com.libreuml.backend.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

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

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("ℹ️ Seed Admin already exists. Skipping.");
            return;
        }

        log.info("🚀 Seeding Admin User from Environment Variables...");

        User admin = Developer.builder()
                .id(UUID.randomUUID())
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .fullName("System Administrator")
                .role(RoleEnum.ADMIN)
                .active(true)
                .joinedAt(LocalDate.now())
                .build();

        userRepository.save(admin);
        log.info("✅ Admin created successfully.");
    }
}