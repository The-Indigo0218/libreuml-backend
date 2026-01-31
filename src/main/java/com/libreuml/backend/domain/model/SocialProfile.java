package com.libreuml.backend.domain.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class SocialProfile {
    private String githubUrl;
    private String instagramUrl;
    private String xUrl;
    private String linkedinUrl;
    private String webSiteUrl;
}
