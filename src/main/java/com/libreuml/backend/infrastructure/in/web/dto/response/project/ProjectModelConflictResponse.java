package com.libreuml.backend.infrastructure.in.web.dto.response.project;

import com.fasterxml.jackson.databind.node.ObjectNode;

public record ProjectModelConflictResponse(String error, String message, long serverVersion, ObjectNode serverData) {}
