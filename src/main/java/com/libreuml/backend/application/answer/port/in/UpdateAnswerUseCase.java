package com.libreuml.backend.application.answer.port.in;

import com.libreuml.backend.application.answer.port.in.dto.*;
import com.libreuml.backend.domain.model.Answer;

public interface UpdateAnswerUseCase {
    Answer updateActiveStatus(UpdateActiveAnswerStatusCommand command);
    Answer updateAcceptStatus(UpdateAcceptAnswerCommand command);
    Answer updateAnswerContent(UpdateAnswerContentCommand command);

}
