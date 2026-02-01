package com.libreuml.backend.application.question.port.in;

import com.libreuml.backend.domain.model.Question;

import java.util.List;
import java.util.UUID;

public interface GetQuestionUseCase {
    Question getQuestionById(UUID id);
    List<Question> getQuestionByTitle(String title);
    List<Question> getQuestionByCreatorId(UUID id);
    List<Question> getActiveQuestions();
}
