package com.libreuml.backend.domain.model;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
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

    private String githubId;
    private String googleId;

    private Instant emailVerifiedAt;

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    @Builder.Default
    private long storageQuotaBytes = 5_242_880L;

    @Builder.Default
    private long storageUsedBytes = 0L;

    public boolean hasQuotaFor(long bytes) {
        return (this.storageUsedBytes + bytes) <= this.storageQuotaBytes;
    }

    public void incrementUsage(long bytes) {
        this.storageUsedBytes += bytes;
    }

    public void decrementUsage(long bytes) {
        this.storageUsedBytes = Math.max(0L, this.storageUsedBytes - bytes);
    }

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