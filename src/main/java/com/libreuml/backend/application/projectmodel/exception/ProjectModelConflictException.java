package com.libreuml.backend.application.projectmodel.exception;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;

@Getter
public class ProjectModelConflictException extends RuntimeException {

    private final long serverVersion;
    private final ObjectNode serverData;

    public ProjectModelConflictException(String message, long serverVersion, ObjectNode serverData) {
        super(message);
        this.serverVersion = serverVersion;
        this.serverData = serverData;
    }
}
