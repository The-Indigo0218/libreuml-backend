package com.libreuml.backend.domain.model.exception;

/**
 * Raised when a project-owned JSON payload (VFS snapshot, semantic model data, or a diagram's
 * view data) exceeds the 5 MB per-document ceiling. Mirrors {@link DiagramPayloadTooLargeException}
 * for the standalone-diagram path.
 */
public class ProjectPayloadTooLargeException extends RuntimeException {
    public ProjectPayloadTooLargeException(String message) {
        super(message);
    }
}
