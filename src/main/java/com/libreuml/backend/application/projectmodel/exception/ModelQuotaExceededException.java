package com.libreuml.backend.application.projectmodel.exception;

import lombok.Getter;

@Getter
public class ModelQuotaExceededException extends RuntimeException {

    private final long used;
    private final long quota;

    public ModelQuotaExceededException(String message, long used, long quota) {
        super(message);
        this.used = used;
        this.quota = quota;
    }
}
