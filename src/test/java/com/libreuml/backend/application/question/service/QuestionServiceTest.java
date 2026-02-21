package com.libreuml.backend.application.question.service;

import com.libreuml.backend.application.question.port.in.dto.CreateQuestionCommand;
import com.libreuml.backend.application.question.port.in.dto.UpdateActiveStatusCommand;
import com.libreuml.backend.application.question.port.in.dto.UpdateSolvedStatusCommand;
import com.libreuml.backend.application.question.port.in.dto.UpdateTitleAndContentCommand;
import com.libreuml.backend.application.question.port.mapper.QuestionMapper;
import com.libreuml.backend.application.question.port.out.QuestionRepository;
import com.libreuml.backend.application.question.port.service.QuestionService;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.Question;
import com.libreuml.backend.domain.model.RoleEnum;
import com.libreuml.backend.domain.model.Student;
import com.libreuml.backend.domain.model.User;
import com.libreuml.backend.application.exception.UserNotAuthorizedException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private QuestionMapper questionMapper;

    @InjectMocks
    private QuestionService questionService;

    private User owner;
    private User stranger;
    private Question question;
    private UUID questionId;

    @BeforeEach
    void setUp() {
        UUID ownerId = UUID.randomUUID();
        owner = Student.builder()
                .id(ownerId)
                .role(RoleEnum.STUDENT)
                .fullName("Owner User")
                .build();

        stranger = Student.builder()
                .id(UUID.randomUUID())
                .role(RoleEnum.STUDENT)
                .fullName("Stranger User")
                .build();

        questionId = UUID.randomUUID();

        question = Question.builder()
                .id(questionId)
                .title("How to use Java?")
                .content("I have a doubt...")
                .creatorId(ownerId)
                .active(true)
                .isSolved(false)
                .build();
    }


    @Test
    @DisplayName("Should create question successfully")
    void createQuestion_Success() {
        CreateQuestionCommand command = new CreateQuestionCommand(
                "New Question",
                "Description",
                owner.getId(),
                List.of(),
                List.of()
        );

        Question newQuestion = Question.builder().title("New Question").build();

        when(userRepository.getUserById(command.creatorId())).thenReturn(Optional.of(owner));
        when(questionMapper.toDomain(command)).thenReturn(newQuestion);
        when(questionRepository.save(newQuestion)).thenReturn(newQuestion);

        Question result = questionService.create(command);

        assertNotNull(result);
        verify(questionRepository).save(newQuestion);
    }


    @Test
    @DisplayName("Should throw exception when user tries to update a question they don't own")
    void updateQuestion_Fail_Security() {

        UpdateTitleAndContentCommand command = new UpdateTitleAndContentCommand(
                questionId,
                "Hacked Title",
                "Hacked Desc",
                stranger.getId()
        );

        when(userRepository.getUserById(command.creatorId())).thenReturn(Optional.of(stranger));
        when(questionRepository.findById(command.id())).thenReturn(Optional.of(question));

        assertThrows(UserNotAuthorizedException.class, () -> {
            questionService.updateTitleAndContent(command);
        });

        verify(questionRepository, never()).save(any());
    }



    @Test
    @DisplayName("Should mark question as resolved when owner requests it")
    void updateSolvedStatus_Success() {

        UpdateSolvedStatusCommand command = new UpdateSolvedStatusCommand(questionId, true, owner.getId());

        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        question.setIsSolved(command.isSolved());
        when(questionRepository.save(question)).thenReturn(question);


        questionService.updateSolvedStatus(command);


        verify(questionRepository).save(question);
        assertTrue(question.getIsSolved());
    }


    @Test
    @DisplayName("Should throw exception when stranger tries to resolve a question")
    void updateSolvedStatus_Fail_Security() {

        UpdateSolvedStatusCommand command = new UpdateSolvedStatusCommand(questionId, true, stranger.getId());

        when(questionRepository.findById(command.id())).thenReturn(Optional.of(question));


        assertThrows(UserNotAuthorizedException.class, () -> {
            questionService.updateSolvedStatus(command);
        });

        verify(questionRepository, never()).save(any());
    }


    @Test
    @DisplayName("Should deactivate question when owner requests it")
    void deactivateQuestion_Success() {

        UpdateActiveStatusCommand command = new UpdateActiveStatusCommand(questionId, owner.getId(), "deactivate");

        when(userRepository.getUserById(owner.getId())).thenReturn(Optional.of(owner));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(questionRepository.save(question)).thenReturn(question);


        questionService.updateActivateStatus(command);

        verify(questionRepository).save(question);
    }


    @Test
    @DisplayName("Should throw exception when stranger tries to deactivate a question")
    void deactivateQuestion_Fail_Security() {

        UpdateActiveStatusCommand command = new UpdateActiveStatusCommand(questionId, stranger.getId(), "deactivate");

        when(userRepository.getUserById(stranger.getId())).thenReturn(Optional.of(stranger));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        assertThrows(UserNotAuthorizedException.class, () -> {
            questionService.updateActivateStatus(command);
        });

        verify(questionRepository, never()).save(any());
    }

}