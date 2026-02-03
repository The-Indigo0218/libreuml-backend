package com.libreuml.backend.application.answer.port.in;

import com.libreuml.backend.application.answer.port.in.dto.GetAnswerByCreatorIdCommand;
import com.libreuml.backend.application.answer.port.in.dto.GetAnswerByQuestionIdCommand;
import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.domain.model.Answer;

import java.util.UUID;

public interface GetAnswerUseCase {
    Answer findById(UUID id);
    PagedResult<Answer> findByQuestionId(GetAnswerByQuestionIdCommand command);
    PagedResult<Answer> findAllByCreatorId(GetAnswerByCreatorIdCommand command);
    PagedResult<Answer> findAllNotAccepted(PaginationCommand command);
    PagedResult<Answer> findAllAcceptedAnswerByQuestionId(GetAnswerByQuestionIdCommand command);
}
