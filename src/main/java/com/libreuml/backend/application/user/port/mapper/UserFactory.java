package com.libreuml.backend.application.user.port.mapper;

import com.libreuml.backend.application.user.port.in.dto.CreateUserCommand;
import com.libreuml.backend.domain.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class UserFactory {

    /**
     * Helper method to instantiate the correct User subclass based on the Role.
     */
    public User buildUser(CreateUserCommand command, String encodedPassword) {
        return switch (command.role()) {
            case STUDENT -> Student.builder()
                    .email(command.email())
                    .password(encodedPassword)
                    .fullName(command.fullName())
                    .role(RoleEnum.STUDENT)
                    .active(true)
                    .joinedAt(LocalDate.now())
                    .build();
            case TEACHER -> Teacher.builder()
                    .email(command.email())
                    .password(encodedPassword)
                    .fullName(command.fullName())
                    .role(RoleEnum.TEACHER)
                    .active(true)
                    .studentCount(0)
                    .joinedAt(LocalDate.now())
                    .build();
            case DEVELOPER -> Developer.builder()
                    .email(command.email())
                    .password(encodedPassword)
                    .fullName(command.fullName())
                    .role(RoleEnum.DEVELOPER)
                    .joinedAt(LocalDate.now())
                    .active(true)
                    .build();
            default -> throw new IllegalArgumentException("Role not supported: " + command.role());
        };
    }
}