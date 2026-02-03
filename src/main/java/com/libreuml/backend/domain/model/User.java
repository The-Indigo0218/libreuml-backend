package com.libreuml.backend.domain.model;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
public abstract class User {

    private UUID id;
    private Boolean active;

    private String email;
    private String password;
    private RoleEnum role;
    private LocalDate lastLogin;

    private String fullName;
    private String avatarUrl;
    private LocalDate joinedAt;

    private List<String> academicDegrees;
    private List<String> organization;
    private List<String> stacks;

    private SocialProfile socialProfile;

    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    public void desactivate() {
        this.active = false;
    }

}