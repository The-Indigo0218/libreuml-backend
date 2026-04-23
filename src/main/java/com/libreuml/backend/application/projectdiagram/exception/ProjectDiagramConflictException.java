package com.libreuml.backend.application.projectdiagram.exception;

import lombok.Getter;

@Getter
public class ProjectDiagramConflictException extends RuntimeException {

    private final long serverVersion;

    public ProjectDiagramConflictException(String message, long serverVersion) {
        super(message);
        this.serverVersion = serverVersion;
    }
}
