package com.libreuml.backend.application.user.port.service;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.application.user.exception.IncorrectPasswordException;
import com.libreuml.backend.application.user.exception.UserAlreadyExistsException;
import com.libreuml.backend.application.user.exception.UserNotFoundException;
import com.libreuml.backend.application.user.port.in.UpdateUserUseCase;
import com.libreuml.backend.application.user.port.in.dto.*;
import com.libreuml.backend.application.user.port.mapper.UserFactory;
import com.libreuml.backend.application.user.port.in.GetUserUseCase;
import com.libreuml.backend.application.user.port.in.LoginUseCase;
import com.libreuml.backend.application.user.port.in.CreateUserUseCase;
import com.libreuml.backend.application.user.port.mapper.UserMapper;
import com.libreuml.backend.application.user.port.out.PasswordEncoderPort;
import com.libreuml.backend.application.user.port.out.TokenProviderPort;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements CreateUserUseCase, GetUserUseCase, LoginUseCase, UpdateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final UserFactory userFactory;
    private final TokenProviderPort tokenProvider;
    private final UserMapper userMapper;

    @Override
    public User create(CreateUserCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new UserAlreadyExistsException("User with email " + command.email() + " already exists.");
        }
        String encodedPassword = encodePassword(command.password());
        User userToSave = userFactory.buildUser(command, encodedPassword);
        return userRepository.save(userToSave);
    }


    @Override
    public User getUserById(UUID id) {
        return userRepository.getUserById(id).orElseThrow(() -> new UserNotFoundException("User with id " + id + " no found"));
    }

    @Override
    public PagedResult<User> getUsersByFullName(String fullName, PaginationCommand pagination) {
        return userRepository.getUserByFullName(fullName, pagination);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public PagedResult<User> getAllUsers(PaginationCommand command) {
        return userRepository.getAllUsers(command);
    }

    @Override
    public PagedResult<User> getActiveUsers(PaginationCommand command) {
        return userRepository.getActiveUsers(command);
    }

    @Override
    public int getActiveUsersCount() {
        return userRepository.getActiveUsersCount();
    }

    @Override
    public int getTotalUsersCount() {
        return userRepository.getTotalUsersCount();
    }

    @Override
    public String login(LoginCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new UserNotFoundException("User with email " + command.email() + " not found"));
        comparePasswords(command.password(), user.getPassword());
        return tokenProvider.generateToken(user);
    }

    @Override
    public User updateUserSocialProfile(UpdateSocialProfileCommand command) {
        User user = getUserOrThrow(command.id());
        userMapper.updateSocialProfileFromCommand(command, user);
        return userRepository.save(user);
    }

    @Override
    public User updateUserProfilePicture(UpdateProfilePictureCommand command) {
        User user = getUserOrThrow(command.id());
        userMapper.updateProfilePictureFromCommand(command, user);
        return userRepository.save(user);
    }

    @Override
    public User updateUserEmail(UpdateEmailCommand command) {
        User user = getUserOrThrow(command.id());
        comparePasswords(command.currentPassword(), user.getPassword());
        if (userRepository.existsByEmail(command.email())) {
            throw new UserAlreadyExistsException("User with email " + command.email() + " already exists.");
        }
        userMapper.updateEmailFromCommand(command, user);
        return userRepository.save(user);
    }

    @Override
    public User updateUserBasicInfo(UpdateUserBasicInfoCommand command) {
        User user = getUserOrThrow(command.id());
        userMapper.updateBasicInfoFromCommand(command, user);
        return userRepository.save(user);
    }

    @Override
    public User updateUserPassword(ChangePasswordCommand command) {
        User user = getUserOrThrow(command.id());
        comparePasswords(command.currentPassword(), user.getPassword());
        String encodedNewPassword = encodePassword(command.newPassword());
        user.changePassword(encodedNewPassword);
        return userRepository.save(user);
    }

    @Override
    public User deactivateUser(DeactivateUserCommand command) {
        User user = getUserOrThrow(command.id());
        user.desactivate();
        return userRepository.save(user);
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id " + userId + " not found"));
    }

    private void comparePasswords(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new IncorrectPasswordException("Incorrect password");
        }
    }

    private String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}