package com.libreuml.backend.application.apikey.exception;

public class ApiKeyOwnershipException extends RuntimeException {
    public ApiKeyOwnershipException(String message) {
        super(message);
    }
}
