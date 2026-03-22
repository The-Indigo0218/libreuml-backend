package com.libreuml.backend.domain.model.exception;

public class AnswerNotBelongToQuestionException extends RuntimeException {
    public AnswerNotBelongToQuestionException(String message) {
        super(message);
    }
}
