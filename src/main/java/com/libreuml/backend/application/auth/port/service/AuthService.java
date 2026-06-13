package com.libreuml.backend.application.auth.port.service;

import com.libreuml.backend.application.auth.dto.IssuedRefreshToken;
import com.libreuml.backend.application.auth.dto.TokenPair;
import com.libreuml.backend.application.auth.port.in.LoginWithRefreshUseCase;
import com.libreuml.backend.application.auth.port.out.RefreshTokenRepository;
import com.libreuml.backend.application.common.port.out.MetricsPort;
import com.libreuml.backend.application.user.exception.AccountDisabledException;
import com.libreuml.backend.application.user.exception.IncorrectPasswordException;
import com.libreuml.backend.application.user.exception.UserNotFoundException;
import com.libreuml.backend.application.user.port.in.dto.LoginCommand;
import com.libreuml.backend.application.user.port.out.PasswordEncoderPort;
import com.libreuml.backend.application.user.port.out.TokenProviderPort;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService implements LoginWithRefreshUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenFactory refreshTokenFactory;
    private final MetricsPort metricsPort;

    @Override
    @Transactional
    public TokenPair login(LoginCommand command, String ipAddress, String userAgent) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> {
                    metricsPort.incrementFailedLogin();
                    return new UserNotFoundException("User not found");
                });

        if (!passwordEncoder.matches(command.password(), user.getPassword())) {
            metricsPort.incrementFailedLogin();
            throw new IncorrectPasswordException("Incorrect password");
        }

        if (Boolean.FALSE.equals(user.getActive())) {
            metricsPort.incrementFailedLogin();
            throw new AccountDisabledException("Account is disabled.");
        }

        metricsPort.incrementActiveUsersDaily("credential");

        String accessToken = tokenProvider.generateToken(user);
        IssuedRefreshToken issued = refreshTokenFactory.issue(user.getId(), ipAddress, userAgent);
        refreshTokenRepository.save(issued.token());

        return new TokenPair(accessToken, issued.rawToken());
    }
}
