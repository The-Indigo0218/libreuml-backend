package com.libreuml.backend.application.auth.exception;

public class InvalidOAuthStateException extends OAuthException {
    public InvalidOAuthStateException(String message) {
        super(message);
    }
}
