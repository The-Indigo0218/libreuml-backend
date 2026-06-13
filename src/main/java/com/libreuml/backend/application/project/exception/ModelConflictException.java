package com.libreuml.backend.application.project.exception;

/**
 * Thrown when the client-supplied semantic-model version does not match the current persisted
 * version, indicating a concurrent edit. Maps to HTTP 409 Conflict.
 */
public class ModelConflictException extends RuntimeException {
    public ModelConflictException(String message) {
        super(message);
    }
}
