package com.libreuml.backend.application.passwordreset.port.service;

import com.libreuml.backend.application.passwordreset.exception.InvalidPasswordResetTokenException;
import com.libreuml.backend.application.passwordreset.port.in.RequestPasswordResetUseCase;
import com.libreuml.backend.application.passwordreset.port.in.ResetPasswordUseCase;
import com.libreuml.backend.application.auth.port.out.RefreshTokenRepository;
import com.libreuml.backend.application.passwordreset.port.out.PasswordResetTokenRepository;
import com.libreuml.backend.application.emailverification.port.out.EmailSenderPort;
import com.libreuml.backend.application.user.exception.UserNotFoundException;
import com.libreuml.backend.application.user.port.out.PasswordEncoderPort;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.PasswordResetToken;
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
public class PasswordResetService implements RequestPasswordResetUseCase, ResetPasswordUseCase {

    private static final int TOKEN_VALIDITY_HOURS = 1;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // A single, generic message for every failure mode (not found / used / expired) so the
    // response never reveals whether a given reset token actually existed or its state.
    private static final String INVALID_RESET_MESSAGE =
            "Invalid or expired reset link. Please request a new one.";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailSenderPort emailSenderPort;
    private final PasswordEncoderPort passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    @Transactional
    public void request(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (isOAuthOnly(user)) {
                emailSenderPort.sendOAuthAccountEmail(email);
                return;
            }

            tokenRepository.deleteAllByUserId(user.getId());

            String rawToken = generateToken();
            PasswordResetToken token = PasswordResetToken.builder()
                    .userId(user.getId())
                    .tokenHash(sha256Hex(rawToken))
                    .expiresAt(Instant.now().plus(TOKEN_VALIDITY_HOURS, ChronoUnit.HOURS))
                    .build();

            tokenRepository.save(token);

            String resetUrl = frontendUrl + "/password/reset?token=" + rawToken;
            emailSenderPort.sendPasswordResetEmail(email, resetUrl);
        });
        // Always returns normally — do not reveal whether the email exists
    }

    @Override
    @Transactional
    public void reset(String rawToken, String newPassword) {
        String hash = sha256Hex(rawToken);

        PasswordResetToken token = tokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidPasswordResetTokenException(INVALID_RESET_MESSAGE));

        if (token.isUsed() || token.isExpired()) {
            throw new InvalidPasswordResetTokenException(INVALID_RESET_MESSAGE);
        }

        User user = userRepository.getUserById(token.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        tokenRepository.save(token);

        // A reset implies the account may be compromised — drop every existing session.
        refreshTokenRepository.deleteAllByUserId(user.getId());
    }

    private boolean isOAuthOnly(User user) {
        boolean hasOAuth = (user.getGithubId() != null) || (user.getGoogleId() != null);
        boolean hasNoPassword = user.getPassword() == null || user.getPassword().isBlank();
        return hasOAuth && hasNoPassword;
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
