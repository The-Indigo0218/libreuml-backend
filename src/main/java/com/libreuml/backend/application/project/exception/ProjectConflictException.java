package com.libreuml.backend.application.project.exception;

/**
 * Thrown when the client-supplied project version does not match the current persisted version,
 * indicating a concurrent edit. Maps to HTTP 409 Conflict.
 */
public class ProjectConflictException extends RuntimeException {
    public ProjectConflictException(String message) {
        super(message);
    }
}
