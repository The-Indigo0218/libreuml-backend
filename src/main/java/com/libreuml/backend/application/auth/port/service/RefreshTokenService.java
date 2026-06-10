package com.libreuml.backend.application.auth.port.service;

import com.libreuml.backend.application.auth.dto.IssuedRefreshToken;
import com.libreuml.backend.application.auth.dto.RefreshCommand;
import com.libreuml.backend.application.auth.dto.TokenPair;
import com.libreuml.backend.application.auth.exception.InvalidRefreshTokenException;
import com.libreuml.backend.application.auth.port.in.RefreshTokenUseCase;
import com.libreuml.backend.application.auth.port.out.RefreshTokenRepository;
import com.libreuml.backend.application.user.exception.UserNotFoundException;
import com.libreuml.backend.application.user.port.out.TokenProviderPort;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.RefreshToken;
import com.libreuml.backend.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService implements RefreshTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenProviderPort tokenProvider;
    private final RefreshTokenFactory refreshTokenFactory;

    @Override
    @Transactional
    public TokenPair refresh(RefreshCommand command) {
        String incomingHash = refreshTokenFactory.hash(command.rawRefreshToken());

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

        refreshTokenRepository.deleteById(existing.getId());

        String accessToken = tokenProvider.generateToken(user);
        IssuedRefreshToken rotated = refreshTokenFactory.issue(user.getId(), command.ipAddress(), command.userAgent());
        refreshTokenRepository.save(rotated.token());

        return new TokenPair(accessToken, rotated.rawToken());
    }

    @Override
    @Transactional
    public void revoke(String rawRefreshToken) {
        String hash = refreshTokenFactory.hash(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash)
                .ifPresent(token -> refreshTokenRepository.deleteById(token.getId()));
    }
}
