package com.libreuml.backend.application.question.port.in;

import com.libreuml.backend.application.question.port.in.dto.CreateQuestionCommand;
import com.libreuml.backend.domain.model.Question;

public interface CreateQuestionUseCase {
    Question create(CreateQuestionCommand command);
}
