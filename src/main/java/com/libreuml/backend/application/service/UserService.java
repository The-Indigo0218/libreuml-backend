package com.libreuml.backend.application.service;

import com.libreuml.backend.application.exception.UserAlreadyExistsException;
import com.libreuml.backend.application.exception.UserNotFoundException;
import com.libreuml.backend.application.mapper.UserFactory;
import com.libreuml.backend.application.port.in.GetUserUseCase;
import com.libreuml.backend.application.port.in.LoginUseCase;
import com.libreuml.backend.application.port.in.dto.CreateUserCommand;
import com.libreuml.backend.application.port.in.CreateUserUseCase;
import com.libreuml.backend.application.port.in.dto.GetUserByIdCommand;
import com.libreuml.backend.application.port.in.dto.LoginCommand;
import com.libreuml.backend.application.port.out.PasswordEncoderPort;
import com.libreuml.backend.application.port.out.TokenProviderPort;
import com.libreuml.backend.application.port.out.UserRepository;
import com.libreuml.backend.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    public User getUserById(GetUserByIdCommand command) {
        return userRepository.getUserById(command.id()).orElseThrow(() -> new UserNotFoundException("User with id " + command.id() + " no found"));
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