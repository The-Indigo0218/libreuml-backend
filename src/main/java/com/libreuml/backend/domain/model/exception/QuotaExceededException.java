package com.libreuml.backend.domain.model.exception;

/**
 * Thrown when a {@link com.libreuml.backend.domain.model.User}'s total storage usage
 * would exceed their assigned quota after a new diagram is added.
 *
 * <p>Maps to HTTP 422 Unprocessable Entity via {@code GlobalControllerAdvice}.
 */
public class QuotaExceededException extends RuntimeException {

    public QuotaExceededException(String message) {
        super(message);
    }
}
