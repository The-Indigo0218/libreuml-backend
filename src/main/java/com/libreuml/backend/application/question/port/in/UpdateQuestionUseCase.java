package com.libreuml.backend.application.question.port.in;

import com.libreuml.backend.application.question.port.in.dto.UpdateQuestionStatusCommand;
import com.libreuml.backend.application.question.port.in.dto.UpdateTitleAndContentCommand;
import com.libreuml.backend.domain.model.Question;

public interface UpdateQuestionUseCase {
    Question updateTitleAndContent(UpdateTitleAndContentCommand command);
    Question updateActiveStatus(UpdateQuestionStatusCommand command);
}
