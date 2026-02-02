package com.libreuml.backend.application.answer.port.out;

import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.domain.model.Answer;
import com.libreuml.backend.application.common.PagedResult;

import java.util.Optional;
import java.util.UUID;

public interface AnswerRepository {

    Answer save(Answer answer);
    Optional<Answer> findById(UUID id);
    PagedResult<Answer> findAllByQuestionId(UUID questionId, PaginationCommand command);
    PagedResult<Answer> findByCreatorId(UUID userId, PaginationCommand command);
    PagedResult<Answer> findAllNotAccepted(PaginationCommand command);
    PagedResult<Answer> findAllAcceptedAnswerByQuestionId(UUID questionId, PaginationCommand command);
}
