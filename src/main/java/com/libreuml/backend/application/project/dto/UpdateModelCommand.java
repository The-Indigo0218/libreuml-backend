package com.libreuml.backend.application.project.dto;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.UUID;

public record UpdateModelCommand(
        UUID projectId,
        UUID requesterId,
        ObjectNode data,
        long version
) {}
