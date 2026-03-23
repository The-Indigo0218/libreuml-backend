package com.libreuml.backend.application.diagram.exception;

public class DiagramNotFoundException extends RuntimeException {
    public DiagramNotFoundException(String message) {
        super(message);
    }
}
