package com.libreuml.backend.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "social_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialProfileEntity {

    @Id
    @UuidGenerator
    private UUID id;

    private String githubUrl;
    private String instagramUrl;
    private String xUrl;
    private String linkedinUrl;
    private String websiteUrl;

    @OneToOne(mappedBy = "socialProfile")
    private UserEntity user;
}