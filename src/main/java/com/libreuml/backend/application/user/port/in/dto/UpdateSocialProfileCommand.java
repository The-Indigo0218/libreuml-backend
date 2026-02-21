package com.libreuml.backend.application.user.port.in.dto;

import java.util.UUID;

public record UpdateSocialProfileCommand(
        UUID id,
        String githubUrl,
        String instagramUrl,
        String xUrl,
        String linkedinUrl,
        String webSiteUrl
) {
}
