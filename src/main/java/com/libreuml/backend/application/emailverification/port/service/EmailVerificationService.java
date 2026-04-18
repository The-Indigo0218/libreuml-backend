package com.libreuml.backend.application.emailverification.port.service;

import com.libreuml.backend.application.emailverification.exception.EmailAlreadyVerifiedException;
import com.libreuml.backend.application.emailverification.exception.InvalidVerificationTokenException;
import com.libreuml.backend.application.emailverification.port.in.ConfirmEmailUseCase;
import com.libreuml.backend.application.emailverification.port.in.SendVerificationEmailUseCase;
import com.libreuml.backend.application.emailverification.port.out.EmailSenderPort;
import com.libreuml.backend.application.emailverification.port.out.EmailVerificationTokenRepository;
import com.libreuml.backend.application.user.exception.UserNotFoundException;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.EmailVerificationToken;
import com.libreuml.backend.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService implements SendVerificationEmailUseCase, ConfirmEmailUseCase {

    private static final int TOKEN_VALIDITY_HOURS = 24;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailSenderPort emailSenderPort;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    @Transactional
    public void send(UUID userId) {
        User user = userRepository.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        if (user.isEmailVerified()) {
            throw new EmailAlreadyVerifiedException("Email is already verified.");
        }

        tokenRepository.deleteAllByUserId(userId);

        String rawToken = generateToken();
        EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(userId)
                .tokenHash(sha256Hex(rawToken))
                .expiresAt(Instant.now().plus(TOKEN_VALIDITY_HOURS, ChronoUnit.HOURS))
                .build();

        tokenRepository.save(token);

        String verificationUrl = frontendUrl + "/email/verify?token=" + rawToken;
        emailSenderPort.sendVerificationEmail(user.getEmail(), verificationUrl);
    }

    @Override
    @Transactional
    public void confirm(String rawToken) {
        String hash = sha256Hex(rawToken);

        EmailVerificationToken token = tokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidVerificationTokenException("Invalid or expired verification token."));

        if (token.isUsed()) {
            throw new InvalidVerificationTokenException("This verification link has already been used.");
        }

        if (token.isExpired()) {
            throw new InvalidVerificationTokenException("This verification link has expired. Please request a new one.");
        }

        User user = userRepository.getUserById(token.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        user.setEmailVerifiedAt(Instant.now());
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        tokenRepository.save(token);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
