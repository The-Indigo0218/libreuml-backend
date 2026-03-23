package com.libreuml.backend.domain.model.exception;

public class DiagramPayloadTooLargeException extends RuntimeException {
    public DiagramPayloadTooLargeException(String message) {
        super(message);
    }
}
