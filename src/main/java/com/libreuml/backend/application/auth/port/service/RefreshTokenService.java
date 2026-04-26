package com.libreuml.backend.application.auth.port.service;

import com.libreuml.backend.application.auth.dto.RefreshCommand;
import com.libreuml.backend.application.auth.dto.TokenPair;
import com.libreuml.backend.application.auth.exception.InvalidRefreshTokenException;
import com.libreuml.backend.application.auth.port.in.RefreshTokenUseCase;
import com.libreuml.backend.application.auth.port.in.SessionUseCase;
import com.libreuml.backend.application.auth.port.out.RefreshTokenRepository;
import com.libreuml.backend.application.user.exception.UserNotFoundException;
import com.libreuml.backend.application.user.port.out.TokenProviderPort;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.RefreshToken;
import com.libreuml.backend.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService implements RefreshTokenUseCase, SessionUseCase {

    private static final int REFRESH_TOKEN_VALIDITY_DAYS = 7;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenProviderPort tokenProvider;

    @Override
    @Transactional
    public TokenPair refresh(RefreshCommand command) {
        String incomingHash = AuthService.sha256Hex(command.rawRefreshToken());

        RefreshToken existing = refreshTokenRepository.findByTokenHash(incomingHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

        if (existing.isRevoked()) {
            // Refresh token reuse detected — a compromised token is being replayed.
            // Revoke the entire session family to limit damage.
            refreshTokenRepository.deleteAllByUserId(existing.getUserId());
            throw new InvalidRefreshTokenException("Refresh token already revoked. All sessions invalidated.");
        }

        if (existing.isExpired()) {
            refreshTokenRepository.deleteById(existing.getId());
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        User user = userRepository.getUserById(existing.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found for refresh token"));

        // Mark as revoked (not deleted) so that a replayed rotation can still be detected.
        refreshTokenRepository.save(existing.revoke());

        String accessToken = tokenProvider.generateToken(user);
        String rawRefreshToken = generateOpaqueToken();

        RefreshToken rotated = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .tokenHash(AuthService.sha256Hex(rawRefreshToken))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(REFRESH_TOKEN_VALIDITY_DAYS, ChronoUnit.DAYS))
                .revoked(false)
                .ipAddress(command.ipAddress())
                .userAgent(command.userAgent())
                .build();

        refreshTokenRepository.save(rotated);

        return new TokenPair(accessToken, rawRefreshToken);
    }

    @Override
    @Transactional
    public void revoke(String rawRefreshToken) {
        String hash = AuthService.sha256Hex(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash)
                .ifPresent(token -> refreshTokenRepository.deleteById(token.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefreshToken> listSessions(UUID userId) {
        return refreshTokenRepository.findAllActiveByUserId(userId);
    }

    @Override
    @Transactional
    public void revokeSession(UUID tokenId, UUID userId) {
        refreshTokenRepository.findByIdAndUserId(tokenId, userId)
                .ifPresent(token -> refreshTokenRepository.save(token.revoke()));
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
