package com.libreuml.backend.domain.model.exception;

public class UserNotAuthorizedException extends RuntimeException {
    public UserNotAuthorizedException(String message) {
        super(message);
    }
}
