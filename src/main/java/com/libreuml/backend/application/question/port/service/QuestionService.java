package com.libreuml.backend.application.question.port.service;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.application.exception.UserNotAuthorizedException;
import com.libreuml.backend.application.question.exception.QuestionNotFoundException;
import com.libreuml.backend.application.question.port.in.CreateQuestionUseCase;
import com.libreuml.backend.application.question.port.in.GetQuestionUseCase;
import com.libreuml.backend.application.question.port.in.UpdateQuestionUseCase;
import com.libreuml.backend.application.question.port.in.dto.UpdateActiveStatusCommand;
import com.libreuml.backend.application.question.port.in.dto.CreateQuestionCommand;
import com.libreuml.backend.application.question.port.in.dto.UpdateSolvedStatusCommand;
import com.libreuml.backend.application.question.port.in.dto.UpdateTitleAndContentCommand;
import com.libreuml.backend.application.question.port.mapper.QuestionMapper;
import com.libreuml.backend.application.question.port.out.QuestionRepository;
import com.libreuml.backend.application.user.exception.UserNotFoundException;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.Question;
import com.libreuml.backend.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionService implements UpdateQuestionUseCase, CreateQuestionUseCase, GetQuestionUseCase {

    private final QuestionRepository questionRepository;
    private final QuestionMapper questionMapper;
    private final UserRepository userRepository;

    @Override
    public Question create(CreateQuestionCommand command) {
        findUserOrThrow(command.creatorId());
        return questionRepository.save(questionMapper.toDomain(command));
    }

    @Override
    public Question updateTitleAndContent(UpdateTitleAndContentCommand command) {
        User user = findUserOrThrow(command.creatorId());
        Question question = findQuestionOrThrow(command.id());
        isUserOwnerOrThrow(question, user.getId());
        questionMapper.updateFromCommand(command, question);
        return questionRepository.save(question);
    }

    @Override
    public Question updateSolvedStatus(UpdateSolvedStatusCommand command) {
        Question question = findQuestionOrThrow(command.id());
        isUserOwnerOrThrow(question, command.creatorId());
        questionMapper.updateFromCommand(command, question);
        return questionRepository.save(question);
    }

    @Override
    public void updateActivateStatus(UpdateActiveStatusCommand command) {
        User user = findUserOrThrow(command.user());
        Question question = findQuestionOrThrow(command.id());
        isUserOwnerOrThrow(question, command.user());
        question.deactivate(user);
        questionRepository.save(question);
    }

    @Override
    public Question getQuestionById(UUID id) {
        return findQuestionOrThrow(id);
    }

    @Override
    public PagedResult<Question> getQuestionByTitle(String title, PaginationCommand paginationCommand) {
        return questionRepository.findAllByTitleContainingIgnoreCase(title, paginationCommand);
    }

    @Override
    public PagedResult<Question> getQuestionByCreatorId(UUID id, PaginationCommand command) {
        return questionRepository.findAllQuestionsByCreatorId(id, command);
    }

    @Override
    public PagedResult<Question> getActiveQuestions(PaginationCommand paginationCommand) {
        return questionRepository.findAllActiveQuestions(paginationCommand);
    }

    private Question findQuestionOrThrow(UUID id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new QuestionNotFoundException("Question with id " + id + " not found"));
    }

    private User findUserOrThrow(UUID id) {
        return userRepository.getUserById(id).orElseThrow(() -> new UserNotFoundException("User with id " + id + " not found"));
    }

    private void isUserOwnerOrThrow(Question question, UUID creatorId) {
        if (!question.getCreatorId().equals(creatorId)) {
            throw new UserNotAuthorizedException("User with id " + creatorId + " is not authorized to update this question");
        }
    }
}
