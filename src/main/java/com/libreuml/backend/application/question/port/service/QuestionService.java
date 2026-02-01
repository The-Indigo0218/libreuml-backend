package com.libreuml.backend.application.question.port.service;

import com.libreuml.backend.application.question.port.exception.QuestionNotFoundException;
import com.libreuml.backend.application.question.port.in.CreateQuestionUseCase;
import com.libreuml.backend.application.question.port.in.GetQuestionUseCase;
import com.libreuml.backend.application.question.port.in.UpdateQuestionUseCase;
import com.libreuml.backend.application.question.port.in.dto.UpdateActiveStatusCommand;
import com.libreuml.backend.application.question.port.in.dto.CreateQuestionCommand;
import com.libreuml.backend.application.question.port.in.dto.UpdateSolvedStatusCommand;
import com.libreuml.backend.application.question.port.in.dto.UpdateTitleAndContentCommand;
import com.libreuml.backend.application.question.port.mapper.QuestionMapper;
import com.libreuml.backend.application.question.port.out.QuestionRepository;
import com.libreuml.backend.application.user.port.exception.UserNotFoundException;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.Question;
import com.libreuml.backend.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionService implements UpdateQuestionUseCase, CreateQuestionUseCase, GetQuestionUseCase {

    private final QuestionRepository questionRepository;
    private final QuestionMapper questionMapper;
    private final UserRepository userRepository;

    @Override
    public Question create(CreateQuestionCommand command) {
        return questionRepository.save(questionMapper.toDomain(command));
    }

    @Override
    public Question updateTitleAndContent(UpdateTitleAndContentCommand command) {
        Question question = findQuestionOrThrow(command.id());
        questionMapper.updateFromCommand(command, question);
        return questionRepository.save(question);
    }

    @Override
    public Question updateSolvedStatus(UpdateSolvedStatusCommand command) {
        Question question = findQuestionOrThrow(command.id());
        questionMapper.updateFromCommand(command, question);
        return questionRepository.save(question);
    }

    @Override
    public void updateActivateStatus(UpdateActiveStatusCommand command) {
        User user = userRepository.getUserById(command.user()).orElseThrow(() -> new UserNotFoundException("User with id " + command.user() + " not found"));
        Question question = findQuestionOrThrow(command.id());
        question.deactivate(user);

        questionRepository.save(question);
    }

    @Override
    public Question getQuestionById(UUID id) {
        return findQuestionOrThrow(id);
    }

    @Override
    public List<Question> getQuestionByTitle(String title) {
        return questionRepository.findAllByTitleContainingIgnoreCase(title);
    }

    @Override
    public List<Question> getQuestionByCreatorId(UUID id) {
        return questionRepository.findAllQuestionsByCreatorId(id);
    }

    @Override
    public List<Question> getActiveQuestions() {
        return questionRepository.findAllActiveQuestions();
    }

    private Question findQuestionOrThrow(UUID id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new QuestionNotFoundException("Question with id " + id + " not found"));
    }
}
