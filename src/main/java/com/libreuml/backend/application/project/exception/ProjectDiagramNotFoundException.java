package com.libreuml.backend.application.project.exception;

public class ProjectDiagramNotFoundException extends RuntimeException {
    public ProjectDiagramNotFoundException(String message) {
        super(message);
    }
}
