package com.libreuml.backend.application.project.exception;

/**
 * Thrown when the client-supplied diagram version does not match the current persisted version,
 * indicating a concurrent edit. Maps to HTTP 409 Conflict.
 */
public class ProjectDiagramConflictException extends RuntimeException {
    public ProjectDiagramConflictException(String message) {
        super(message);
    }
}
