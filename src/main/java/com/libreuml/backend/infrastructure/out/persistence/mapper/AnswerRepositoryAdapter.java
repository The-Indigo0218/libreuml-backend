package com.libreuml.backend.infrastructure.out.persistence.mapper;

import com.libreuml.backend.application.answer.port.out.AnswerRepository;
import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.domain.model.Answer;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
@Component
public class AnswerRepositoryAdapter implements AnswerRepository {
    @Override
    public Answer save(Answer answer) {
        return null;
    }

    @Override
    public Optional<Answer> findById(UUID id) {
        return Optional.empty();
    }

    @Override
    public PagedResult<Answer> findAllByQuestionId(UUID questionId, PaginationCommand command) {
        return null;
    }

    @Override
    public PagedResult<Answer> findByCreatorId(UUID userId, PaginationCommand command) {
        return null;
    }

    @Override
    public PagedResult<Answer> findAllNotAccepted(PaginationCommand command) {
        return null;
    }

    @Override
    public PagedResult<Answer> findAllAcceptedAnswerByQuestionId(UUID questionId, PaginationCommand command) {
        return null;
    }
}
