package com.libreuml.backend.application.exception;

public class AnswerNotBelongToQuestionException extends RuntimeException {
    public AnswerNotBelongToQuestionException(String message) {
        super(message);
    }
}
