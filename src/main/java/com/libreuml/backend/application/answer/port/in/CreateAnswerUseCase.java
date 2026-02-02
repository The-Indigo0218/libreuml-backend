package com.libreuml.backend.application.answer.port.in;

import com.libreuml.backend.application.answer.port.in.dto.CreateAnswerCommand;
import com.libreuml.backend.domain.model.Answer;
import com.libreuml.backend.domain.model.Question;

public interface CreateAnswerUseCase {
    Answer createAnswer(CreateAnswerCommand command);
}
