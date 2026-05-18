package com.libreuml.backend.application.apikey.exception;

public class RedemptionLimitExceededException extends RuntimeException {
    public RedemptionLimitExceededException(String message) {
        super(message);
    }
}
