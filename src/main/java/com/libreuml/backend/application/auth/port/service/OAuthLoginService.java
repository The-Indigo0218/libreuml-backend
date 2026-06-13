package com.libreuml.backend.application.auth.port.service;

import com.libreuml.backend.application.auth.dto.IssuedRefreshToken;
import com.libreuml.backend.application.auth.dto.OAuthCallbackCommand;
import com.libreuml.backend.application.auth.dto.OAuthProvider;
import com.libreuml.backend.application.auth.dto.OAuthUserInfo;
import com.libreuml.backend.application.auth.dto.TokenPair;
import com.libreuml.backend.application.auth.exception.OAuthException;
import com.libreuml.backend.application.auth.port.in.OAuthLoginUseCase;
import com.libreuml.backend.application.auth.port.out.OAuthProviderPort;
import com.libreuml.backend.application.auth.port.out.OAuthStatePort;
import com.libreuml.backend.application.auth.port.out.RefreshTokenRepository;
import com.libreuml.backend.application.common.port.out.MetricsPort;
import com.libreuml.backend.application.user.port.out.PasswordEncoderPort;
import com.libreuml.backend.application.user.port.out.TokenProviderPort;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.Developer;
import com.libreuml.backend.domain.model.RoleEnum;
import com.libreuml.backend.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuthLoginService implements OAuthLoginUseCase {

    private final List<OAuthProviderPort> providerAdapters;
    private final OAuthStatePort statePort;
    private final UserRepository userRepository;
    private final TokenProviderPort tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenFactory refreshTokenFactory;
    private final PasswordEncoderPort passwordEncoder;
    private final MetricsPort metricsPort;

    @Override
    @Transactional
    public TokenPair login(OAuthCallbackCommand command) {
        statePort.validateState(command.state());

        OAuthUserInfo userInfo = resolveAdapter(command.provider())
                .fetchUserInfo(command.code(), command.redirectUri());

        User user = findOrProvision(userInfo, command.provider());
        String providerName = command.provider().name().toLowerCase();
        metricsPort.incrementOAuthLogin(providerName);
        metricsPort.incrementActiveUsersDaily(providerName);

        return issueTokens(user, command.ipAddress(), command.userAgent());
    }

    private User findOrProvision(OAuthUserInfo userInfo, OAuthProvider provider) {
        // 1. Primary lookup: immutable provider ID (stable even if email changes)
        Optional<User> byProviderId = switch (provider) {
            case GITHUB -> userRepository.findByGithubId(userInfo.providerId());
            case GOOGLE -> userRepository.findByGoogleId(userInfo.providerId());
        };
        if (byProviderId.isPresent()) {
            return byProviderId.get();
        }

        // 2. Email-based account linking (only with a verified email to prevent hijacking)
        if (userInfo.emailVerified() && userInfo.email() != null) {
            Optional<User> byEmail = userRepository.findByEmail(userInfo.email());
            if (byEmail.isPresent()) {
                User existing = byEmail.get();
                linkProvider(existing, provider, userInfo.providerId());
                return userRepository.save(existing);
            }
        }

        // 3. Auto-provision: first time this identity is seen
        return registerOAuthUser(userInfo, provider);
    }

    private void linkProvider(User user, OAuthProvider provider, String providerId) {
        switch (provider) {
            case GITHUB -> user.linkGithub(providerId);
            case GOOGLE -> user.linkGoogle(providerId);
        }
    }

    private User registerOAuthUser(OAuthUserInfo userInfo, OAuthProvider provider) {
        if (userInfo.email() == null || userInfo.email().isBlank()) {
            throw new OAuthException(
                "OAuth provider did not return an email address. " +
                "Please ensure your " + provider + " account has a public primary email."
            );
        }

        // OAuth users get an encoded random password — it can never be used for /login.
        String unusablePassword = passwordEncoder.encode(UUID.randomUUID().toString());

        Developer.DeveloperBuilder<?, ?> builder = Developer.builder()
                .email(userInfo.email())
                .fullName(userInfo.name() != null ? userInfo.name() : userInfo.email())
                .avatarUrl(userInfo.avatarUrl())
                .password(unusablePassword)
                .role(RoleEnum.DEVELOPER)
                .active(true)
                .joinedAt(LocalDate.now())
                .passwordVersion(0);

        if (userInfo.emailVerified()) {
            builder.emailVerifiedAt(Instant.now());
        }

        Developer newUser = switch (provider) {
            case GITHUB -> builder.githubId(userInfo.providerId()).build();
            case GOOGLE -> builder.googleId(userInfo.providerId()).build();
        };

        return userRepository.save(newUser);
    }

    private TokenPair issueTokens(User user, String ipAddress, String userAgent) {
        String accessToken = tokenProvider.generateToken(user);
        IssuedRefreshToken issued = refreshTokenFactory.issue(user.getId(), ipAddress, userAgent);
        refreshTokenRepository.save(issued.token());
        return new TokenPair(accessToken, issued.rawToken());
    }

    private OAuthProviderPort resolveAdapter(OAuthProvider provider) {
        return providerAdapters.stream()
                .filter(a -> a.provider() == provider)
                .findFirst()
                .orElseThrow(() -> new OAuthException("No adapter registered for provider: " + provider));
    }
}
