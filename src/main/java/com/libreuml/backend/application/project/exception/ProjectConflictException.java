package com.libreuml.backend.application.project.exception;

import lombok.Getter;

@Getter
public class ProjectConflictException extends RuntimeException {

    private final long serverVersion;

    public ProjectConflictException(String message, long serverVersion) {
        super(message);
        this.serverVersion = serverVersion;
    }
}
