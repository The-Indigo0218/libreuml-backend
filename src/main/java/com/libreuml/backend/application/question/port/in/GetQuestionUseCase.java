package com.libreuml.backend.application.question.port.in;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.domain.model.Question;

import java.util.List;
import java.util.UUID;

public interface GetQuestionUseCase {
    Question getQuestionById(UUID id);
    PagedResult<Question> getQuestionByTitle(String title, PaginationCommand paginationCommand);
    PagedResult<Question> getQuestionByCreatorId(UUID id, PaginationCommand paginationCommand);
    PagedResult<Question> getActiveQuestions(PaginationCommand paginationCommand);
}
