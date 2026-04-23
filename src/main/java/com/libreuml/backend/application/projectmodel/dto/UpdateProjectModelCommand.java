package com.libreuml.backend.application.projectmodel.dto;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.UUID;

public record UpdateProjectModelCommand(
        UUID projectId,
        UUID requesterId,
        ObjectNode data,
        long version
) {}
