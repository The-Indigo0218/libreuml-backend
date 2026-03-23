package com.libreuml.backend.application.diagram.exception;

/**
 * Thrown when the client-supplied version does not match the current persisted version,
 * indicating that the diagram was modified by a concurrent request.  Maps to HTTP 409 Conflict.
 */
public class DiagramConflictException extends RuntimeException {
    public DiagramConflictException(String message) {
        super(message);
    }
}
