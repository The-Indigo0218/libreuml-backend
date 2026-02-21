package com.libreuml.backend.infrastructure.in.web.dto.request.user;

import org.hibernate.validator.constraints.URL;

public record UpdaterSocialProfileRequest(
        @URL(message = "Invalid GitHub URL")
        String githubUrl,

        @URL(message = "Invalid Instagram URL")
        String instagramUrl,

        @URL(message = "Invalid X (Twitter) URL")
        String xUrl,

        @URL(message = "Invalid LinkedIn URL")
        String linkedinUrl,

        @URL(message = "Invalid Website URL")
        String webSiteUrl
) {}