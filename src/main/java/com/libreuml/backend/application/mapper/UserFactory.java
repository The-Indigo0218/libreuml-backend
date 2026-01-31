package com.libreuml.backend.application.mapper;

import com.libreuml.backend.application.port.in.dto.CreateUserCommand;
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
                    .build();
            case TEACHER -> Teacher.builder()
                    .email(command.email())
                    .password(encodedPassword)
                    .fullName(command.fullName())
                    .role(RoleEnum.TEACHER)
                    .active(true)
                    .studentCount(0)
                    .build();
            case DEVELOPER -> Developer.builder()
                    .email(command.email())
                    .password(encodedPassword)
                    .fullName(command.fullName())
                    .role(RoleEnum.DEVELOPER)
                    .active(true)
                    .build();
            default -> throw new IllegalArgumentException("Role not supported: " + command.role());
        };
    }
}