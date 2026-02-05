package com.libreuml.backend.infrastructure.out.persistence.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialProfileEmbeddable {
    private String githubUrl;
    private String instagramUrl;
    private String xUrl;
    private String linkedinUrl;
    private String webSiteUrl;
}