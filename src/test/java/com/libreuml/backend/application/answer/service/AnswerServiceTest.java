package com.libreuml.backend.application.answer.service;

import com.libreuml.backend.application.answer.port.in.dto.CreateAnswerCommand;
import com.libreuml.backend.application.answer.port.in.dto.UpdateAcceptAnswerCommand;
import com.libreuml.backend.application.answer.port.in.dto.UpdateAnswerContentCommand;
import com.libreuml.backend.application.answer.port.mapper.AnswerMapper;
import com.libreuml.backend.application.answer.port.out.AnswerRepository;
import com.libreuml.backend.application.answer.port.service.AnswerService;
import com.libreuml.backend.application.question.port.out.QuestionRepository;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.*;
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
class AnswerServiceTest {

    @Mock
    private AnswerRepository answerRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AnswerMapper answerMapper;

    @InjectMocks
    private AnswerService answerService;

    private User questionOwner;
    private User answerAuthor;
    private User stranger;

    private Question question;
    private Answer answer;
    private UUID questionId;
    private UUID answerId;

    @BeforeEach
    void setUp() {
        UUID questionOwnerId = UUID.randomUUID();
        UUID answerAuthorId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        questionId = UUID.randomUUID();
        answerId = UUID.randomUUID();

        questionOwner = Student.builder().id(questionOwnerId).fullName("Question Owner").role(RoleEnum.STUDENT).build();
        answerAuthor = Student.builder().id(answerAuthorId).fullName("Answer Author").role(RoleEnum.STUDENT).build();
        stranger = Student.builder().id(strangerId).fullName("Stranger").role(RoleEnum.STUDENT).build();

        question = Question.builder()
                .id(questionId)
                .title("Java Help")
                .creatorId(questionOwnerId)
                .isSolved(false)
                .active(true)
                .build();

        answer = Answer.builder()
                .id(answerId)
                .content("Use Mockito")
                .creatorId(answerAuthorId)
                .questionId(questionId)
                .isAccepted(false)
                .build();
    }

    @Test
    @DisplayName("Should create answer successfully when question exists")
    void createAnswer_Success() {

        CreateAnswerCommand command = new CreateAnswerCommand(answerAuthor.getId(), questionId, "Use Mockito", List.of() );
        Answer newAnswer = Answer.builder().content("Use Mockito").build();


        when(userRepository.getUserById(answerAuthor.getId())).thenReturn(Optional.of(answerAuthor));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(answerMapper.toDomain(command)).thenReturn(newAnswer);
        when(answerRepository.save(newAnswer)).thenReturn(newAnswer);


        Answer result = answerService.createAnswer(command);


        assertNotNull(result);
        verify(answerRepository).save(newAnswer);
    }

    @Test
    @DisplayName("Should throw exception when trying to answer a non-existent question")
    void createAnswer_Fail_QuestionNotFound() {

        CreateAnswerCommand command = new CreateAnswerCommand( answerAuthor.getId(), questionId, "Use Mockito", List.of() );

        when(questionRepository.findById(command.questionId())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> answerService.createAnswer(command));

        verify(answerRepository, never()).save(any());
    }


    @Test
    @DisplayName("Should accept answer and mark question as resolved when Question Owner requests it")
    void acceptAnswer_Success() {

        UpdateAcceptAnswerCommand command = new UpdateAcceptAnswerCommand( questionOwner.getId(), answerId);

        when(userRepository.getUserById(command.userId())).thenReturn(Optional.of(questionOwner));
        when(answerRepository.findById(command.answerId())).thenReturn(Optional.of(answer));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        when(answerRepository.save(answer)).thenReturn(answer);


        answerService.updateAcceptStatus(command);



        assertTrue(answer.getIsAccepted(), "Answer should be marked as accepted");
        assertTrue(question.getIsSolved(), "Question should be marked as solved");

        verify(answerRepository).save(answer);
         verify(questionRepository).save(question);
    }


    @Test
    @DisplayName("Should throw exception when Answer Owner tries to accept their own answer")
    void acceptAnswer_Fail_AnswerOwnerTriesToAccept() {

        UpdateAcceptAnswerCommand command = new UpdateAcceptAnswerCommand( answerAuthor.getId(), answerId);

        when(userRepository.getUserById(answerAuthor.getId())).thenReturn(Optional.of(answerAuthor));
        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));


        assertThrows(UserNotAuthorizedException.class, () -> answerService.updateAcceptStatus(command));

        assertFalse(answer.getIsAccepted());
        verify(answerRepository, never()).save(any());
    }


    @Test
    @DisplayName("Should throw exception when Stranger tries to accept an answer")
    void acceptAnswer_Fail_StrangerTriesToAccept() {

        UpdateAcceptAnswerCommand command = new UpdateAcceptAnswerCommand(stranger.getId(), answerId );

        when(userRepository.getUserById(stranger.getId())).thenReturn(Optional.of(stranger));
        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));


        assertThrows(UserNotAuthorizedException.class, () -> answerService.updateAcceptStatus(command));

        verify(answerRepository, never()).save(any());
    }


    @Test
    @DisplayName("Should throw exception when Stranger tries to edit an answer")
    void updateAnswer_Fail_Security() {

        UpdateAnswerContentCommand command = new UpdateAnswerContentCommand(answerId,  stranger.getId(), "Hacked content");

        when(userRepository.getUserById(stranger.getId())).thenReturn(Optional.of(stranger));
        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));


        assertThrows(UserNotAuthorizedException.class, () -> answerService.updateAnswerContent(command));

        verify(answerRepository, never()).save(any());
    }
}