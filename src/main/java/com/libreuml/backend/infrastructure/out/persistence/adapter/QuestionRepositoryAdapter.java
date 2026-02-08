package com.libreuml.backend.infrastructure.out.persistence.adapter;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.application.question.port.out.QuestionRepository;
import com.libreuml.backend.domain.model.Question;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class QuestionRepositoryAdapter implements QuestionRepository {
    @Override
    public Optional<Question> findById(UUID id) {
        return Optional.empty();
    }

    @Override
    public Question save(Question question) {
        return null;
    }

    @Override
    public PagedResult<Question> findAllActiveQuestions(PaginationCommand paginationCommand) {
        return null;
    }

    @Override
    public PagedResult<Question> findAllQuestionsByCreatorId(UUID creatorId, PaginationCommand paginationCommand) {
        return null;
    }

    @Override
    public PagedResult<Question> findAllByTitleContainingIgnoreCase(String title, PaginationCommand paginationCommand) {
        return null;
    }

    @Override
    public PagedResult<Question> findAllByTag(String tag, PaginationCommand paginationCommand) {
        return null;
    }
}
