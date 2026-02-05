package com.libreuml.backend.application.answer.port.service;

import com.libreuml.backend.application.answer.port.in.CreateAnswerUseCase;
import com.libreuml.backend.application.answer.port.in.GetAnswerUseCase;
import com.libreuml.backend.application.answer.port.in.UpdateAnswerUseCase;
import com.libreuml.backend.application.answer.port.in.dto.*;
import com.libreuml.backend.application.answer.port.mapper.AnswerMapper;
import com.libreuml.backend.application.answer.port.out.AnswerRepository;
import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.application.exception.UserNotAuthorizedException;
import com.libreuml.backend.application.question.exception.QuestionNotFoundException;
import com.libreuml.backend.application.question.port.out.QuestionRepository;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.Answer;
import com.libreuml.backend.domain.model.Question;
import com.libreuml.backend.domain.model.RoleEnum;
import com.libreuml.backend.domain.model.User;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnswerService implements GetAnswerUseCase, CreateAnswerUseCase, UpdateAnswerUseCase {

    final QuestionRepository questionRepository;
    final AnswerRepository answerRepository;
    final UserRepository userRepository;
    final AnswerMapper answerMapper;

    @Override
    public Answer createAnswer(CreateAnswerCommand command) {
        getQuestionOrThrow(command.questionId());
        getUserOrThrow(command.creatorId());
        Answer answer = answerMapper.toDomain(command);
        return answerRepository.save(answer);
    }

    @Override
    public Answer findById(UUID id) {
        return getAnswerOrThrow(id);
    }

    @Override
    public PagedResult<Answer> findByQuestionId(GetAnswerByQuestionIdCommand command) {
        getQuestionOrThrow(command.questionId());
        return answerRepository.findAllByQuestionId(command.questionId(), command.paginationCommand());
    }

    @Override
    public PagedResult<Answer> findAllByCreatorId(GetAnswerByCreatorIdCommand command) {
        getUserOrThrow(command.creatorId());
        return answerRepository.findByCreatorId(command.creatorId(), command.paginationCommand());
    }

    @Override
    public PagedResult<Answer> findAllNotAccepted(PaginationCommand command) {
        return answerRepository.findAllNotAccepted(command);
    }

    @Override
    public PagedResult<Answer> findAllAcceptedAnswerByQuestionId(GetAnswerByQuestionIdCommand command) {
        getQuestionOrThrow(command.questionId());
        return answerRepository.findAllAcceptedAnswerByQuestionId(command.questionId(), command.paginationCommand());
    }

    @Override
    public Answer updateActiveStatus(UpdateActiveAnswerStatusCommand command) {
        User user = getUserOrThrow(command.user());
        Answer answer = getAnswerOrThrow(command.id());
        answer.deactivate(user);
        return answerRepository.save(answer);
    }

    @Override
    @Transactional
    public Answer updateAcceptStatus(UpdateAcceptAnswerCommand command) {
        User user = getUserOrThrow(command.userId());
        Answer answer = getAnswerOrThrow(command.answerId());
        Question question = getQuestionOrThrow(answer.getQuestionId());
        if (!question.getCreatorId().equals(user.getId()) && !user.getRole().equals(RoleEnum.MODERATOR)) {
            throw new UserNotAuthorizedException("User is not authorized to accept this answer");
        }
        question.resolve(user);
        answer.accept();
        questionRepository.save(question);
        return answerRepository.save(answer);
    }

    @Override
    public Answer updateAnswerContent(UpdateAnswerContentCommand command) {
         getUserOrThrow(command.user());
        Answer answer = getAnswerOrThrow(command.id());
        if (!answer.getCreatorId().equals(command.user())) {
            throw new UserNotAuthorizedException("User is not authorized to update this answer");
        }
        answerMapper.updateContentFromCommand(command, answer);
        return answerRepository.save(answer);
    }

    private Answer getAnswerOrThrow(UUID answerId) {
        return answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Answer with id " + answerId + " not found"));
    }

    private Question getQuestionOrThrow(UUID questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException("Question with id " + questionId + " not found"));
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User with id " + userId + " not found"));
    }
}
