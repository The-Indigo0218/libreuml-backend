package com.libreuml.backend.application.user.service;

import com.libreuml.backend.application.user.exception.IncorrectPasswordException;
import com.libreuml.backend.application.user.exception.UserAlreadyExistsException;
import com.libreuml.backend.application.user.exception.UserNotFoundException;
import com.libreuml.backend.application.user.port.in.dto.ChangePasswordCommand;
import com.libreuml.backend.application.user.port.in.dto.CreateUserCommand;
import com.libreuml.backend.application.user.port.in.dto.LoginCommand;
import com.libreuml.backend.application.user.port.in.dto.UpdateEmailCommand;
import com.libreuml.backend.application.user.port.mapper.UserFactory;
import com.libreuml.backend.application.user.port.mapper.UserMapper;
import com.libreuml.backend.application.user.port.out.PasswordEncoderPort;
import com.libreuml.backend.application.user.port.out.TokenProviderPort;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.application.user.port.service.UserService;
import com.libreuml.backend.domain.model.RoleEnum;
import com.libreuml.backend.domain.model.Teacher;
import com.libreuml.backend.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;
    @Mock
    private UserFactory userFactory;
    @Mock
    private TokenProviderPort tokenProvider;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    //Test Object
    private User teacher;
    private CreateUserCommand createUserCommand;

    @BeforeEach
    void setUp() {
        teacher = Teacher.builder()
                .id(java.util.UUID.randomUUID())
                .fullName("John Doe")
                .email("email@email.com")
                .password("encodedPassword")
                .role(RoleEnum.TEACHER)
                .build();

        createUserCommand = new CreateUserCommand(
                "email@email.com",
                "encodedPassword",
                "John Doe",
                RoleEnum.TEACHER
        );
    }


    @Test
    @DisplayName("Should create user successfully when email is unique")
    void createUser_Success() {
        when(userRepository.existsByEmail(createUserCommand.email())).thenReturn(false);
        when(passwordEncoder.encode(createUserCommand.password())).thenReturn("encodedPassword");
        when(userFactory.buildUser(createUserCommand, "encodedPassword")).thenReturn(teacher);
        when(userRepository.save(teacher)).thenReturn(teacher);

        User createdUser = userService.create(createUserCommand);

        assertNotNull(createdUser);
        assertEquals(teacher.getEmail(), createdUser.getEmail());
        assertEquals(teacher.getFullName(), createdUser.getFullName());

        verify(passwordEncoder).encode(createUserCommand.password());
        verify(userRepository).save(teacher);
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void createUser_Fail_EmailExists() {
        when(userRepository.existsByEmail(createUserCommand.email())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> {
            userService.create(createUserCommand);
        });

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return token when credentials are correct")
    void login_Success() {
        String rawPassword = "rawPassword123";
        String encodedPassword = "encodedPassword123";
        String expectedToken = "eyJhbGciOi...";

        User userInDb = teacher;
        userInDb.setPassword(encodedPassword);

        LoginCommand loginCommand = new LoginCommand("email@email.com", rawPassword);

        when(userRepository.findByEmail(loginCommand.email())).thenReturn(Optional.of(userInDb));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
        when(tokenProvider.generateToken(userInDb)).thenReturn(expectedToken);

        String actualToken = userService.login(loginCommand);

        assertEquals(expectedToken, actualToken);
        verify(tokenProvider).generateToken(userInDb);
    }

    @Test
    @DisplayName("Should throw exception when password does not match")
    void login_Fail_WrongPassword() {
        String rawPassword = "wrongPassword";
        String encodedPassword = "encodedPassword123";

        User userInDb = teacher;
        userInDb.setPassword(encodedPassword);

        LoginCommand loginCommand = new LoginCommand("email@email.com", rawPassword);

        when(userRepository.findByEmail(loginCommand.email())).thenReturn(Optional.of(userInDb));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(false);

        assertThrows(IncorrectPasswordException.class, () -> {
            userService.login(loginCommand);
        });

        verify(tokenProvider, never()).generateToken(any());
    }

    @Test
    @DisplayName("Should throw exception when user not found by email during login")
    void login_Fail_UserNotFound() {
        LoginCommand loginCommand = new LoginCommand("emailfail@emailFail.com", "somePassword");

        when(userRepository.findByEmail(loginCommand.email())).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> {
            userService.login(loginCommand);
        });
        verify(passwordEncoder, never()).encode(any());
        verify(passwordEncoder, never()).matches(any(), any());
        verify(tokenProvider, never()).generateToken(any());
    }

    @Test
    @DisplayName("Should throw exception when user try update email with incorrect password")
    void updateUser_Fail_IncorrectPassword() {
        UpdateEmailCommand updateEmailCommand = new UpdateEmailCommand(teacher.getId(), "emailnew@email.com", "rawPassword1238");

        when(userRepository.getUserById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(passwordEncoder.matches(updateEmailCommand.currentPassword(), teacher.getPassword())).thenReturn(false);

        assertThrows(IncorrectPasswordException.class, () -> {
            userService.updateUserEmail(updateEmailCommand);
        });

        verify(passwordEncoder).matches(updateEmailCommand.currentPassword(), teacher.getPassword());
        verify(userRepository, never()).save(any());
        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository, never()).save(any());

    }

    @Test
    @DisplayName("Should throw exception when user try update email to an existing email")
    void updateUser_Fail_EmailAlreadyExists() {
        UpdateEmailCommand updateEmailCommand = new UpdateEmailCommand(teacher.getId(), "emailNew@email.com", "rawPassword123");

        when(userRepository.getUserById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(passwordEncoder.matches(updateEmailCommand.currentPassword(), teacher.getPassword())).thenReturn(true);
        when(userRepository.existsByEmail(updateEmailCommand.email())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> {
            userService.updateUserEmail(updateEmailCommand);
        });

        verify(passwordEncoder).matches(updateEmailCommand.currentPassword(), teacher.getPassword());
        verify(userRepository).existsByEmail(updateEmailCommand.email());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when current password is incorrect during password update")
    void updateUser_Fail_IncorrectCurrentPassword() {
        ChangePasswordCommand changePasswordCommand = new ChangePasswordCommand(teacher.getId(), "oldPassword123", "newPassword123");
        when(userRepository.getUserById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(passwordEncoder.matches(changePasswordCommand.currentPassword(), teacher.getPassword())).thenReturn(false);

        assertThrows(IncorrectPasswordException.class, () -> {
            userService.updateUserPassword(changePasswordCommand);
        });

        verify(passwordEncoder).matches(changePasswordCommand.currentPassword(), teacher.getPassword());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when get user by id that does not exist")
    void getUserById_Fail_UserNotFound() {
        java.util.UUID nonExistentUserId = java.util.UUID.randomUUID();
        when(userRepository.getUserById(nonExistentUserId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            userService.getUserById(nonExistentUserId);
        });
        verify(userRepository).getUserById(nonExistentUserId);
    }

    @Test
    @DisplayName("Should update password successfully when current password is correct")
    void updateUserPassword_Success() {
        ChangePasswordCommand command = new ChangePasswordCommand(teacher.getId(), "oldPass", "newPass");
        String encodedNewPass = "encodedNewPass";

        when(userRepository.getUserById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(passwordEncoder.matches(command.currentPassword(), teacher.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(command.newPassword())).thenReturn(encodedNewPass);
        when(userRepository.save(teacher)).thenReturn(teacher);
        userService.updateUserPassword(command);

        verify(passwordEncoder).encode(command.newPassword());
        verify(userRepository).save(teacher);

        assertEquals(encodedNewPass, teacher.getPassword());
    }

    @Test
    @DisplayName("Should update email successfully when password is correct and email is unique")
    void updateUserEmail_Success() {
        UpdateEmailCommand command = new UpdateEmailCommand(teacher.getId(), "newemail@test.com", "correctPass");

        when(userRepository.getUserById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(passwordEncoder.matches(command.currentPassword(), teacher.getPassword())).thenReturn(true);
        when(userRepository.existsByEmail(command.email())).thenReturn(false);
        when(userRepository.save(teacher)).thenReturn(teacher);

        userService.updateUserEmail(command);

        verify(userMapper).updateEmailFromCommand(command, teacher);
        verify(userRepository).save(teacher);
    }

}
