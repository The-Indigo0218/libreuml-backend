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

    private int passwordVersion;

    // Nullable: set only when the user has authenticated via the respective OAuth provider.
    private String githubId;
    private String googleId;

    public void changePassword(String newPassword) {
        this.password = newPassword;
        this.passwordVersion++;
    }

    public void desactivate() {
        this.active = false;
    }

    public void linkGithub(String githubId) {
        this.githubId = githubId;
    }

    public void linkGoogle(String googleId) {
        this.googleId = googleId;
    }

}