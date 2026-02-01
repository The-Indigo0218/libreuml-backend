package com.libreuml.backend.application.user.port.service;

import com.libreuml.backend.application.user.port.exception.UserAlreadyExistsException;
import com.libreuml.backend.application.user.port.exception.UserNotFoundException;
import com.libreuml.backend.application.user.port.mapper.UserFactory;
import com.libreuml.backend.application.user.port.in.GetUserUseCase;
import com.libreuml.backend.application.user.port.in.LoginUseCase;
import com.libreuml.backend.application.user.port.in.dto.CreateUserCommand;
import com.libreuml.backend.application.user.port.in.CreateUserUseCase;
import com.libreuml.backend.application.user.port.in.dto.LoginCommand;
import com.libreuml.backend.application.user.port.out.PasswordEncoderPort;
import com.libreuml.backend.application.user.port.out.TokenProviderPort;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements CreateUserUseCase, GetUserUseCase, LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final UserFactory  userFactory;
    private final TokenProviderPort  tokenProvider;

    @Override
    public User create(CreateUserCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new UserAlreadyExistsException("User with email " + command.email() + " already exists.");
        }

        String encodedPassword = passwordEncoder.encode(command.password());

        User userToSave = userFactory.buildUser(command, encodedPassword);

        return userRepository.save(userToSave);
    }


    @Override
    public User getUserById(UUID id) {
        return userRepository.getUserById(id).orElseThrow(() -> new UserNotFoundException("User with id " + id + " no found"));
    }

    @Override
    public List<User> getUserByFullName(String fullName) {
        return userRepository.getUserByFullName(fullName);
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User with email " + email + " no found"));
    }

    @Override
    public String login(LoginCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new RuntimeException("Invalid Credentials"));
        if (!passwordEncoder.matches(command.password(), user.getPassword())) {
            throw new RuntimeException("Invalid Credentials");
        }
        return tokenProvider.generateToken(user);    }
}