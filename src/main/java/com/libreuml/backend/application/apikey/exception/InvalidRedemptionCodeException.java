package com.libreuml.backend.application.apikey.exception;

public class InvalidRedemptionCodeException extends RuntimeException {
    public InvalidRedemptionCodeException(String message) {
        super(message);
    }
}
