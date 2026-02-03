package com.libreuml.backend.application.question.port.out;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.domain.model.Question;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository {
    Optional<Question> findById(UUID id);
    Question save(Question question);
    PagedResult<Question> findAllActiveQuestions(PaginationCommand paginationCommand);
    PagedResult<Question> findAllQuestionsByCreatorId(UUID creatorId, PaginationCommand paginationCommand);
    PagedResult<Question> findAllByTitleContainingIgnoreCase(String title, PaginationCommand paginationCommand);
    PagedResult<Question> findAllByTag(String tag, PaginationCommand paginationCommand);
}
