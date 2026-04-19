package com.libreuml.backend.infrastructure.in.web.controller;

import com.libreuml.backend.application.auth.dto.RefreshCommand;
import com.libreuml.backend.application.auth.dto.TokenPair;
import com.libreuml.backend.application.auth.port.in.LoginWithRefreshUseCase;
import com.libreuml.backend.application.auth.port.in.RefreshTokenUseCase;
import com.libreuml.backend.application.emailverification.port.in.ConfirmEmailUseCase;
import com.libreuml.backend.application.emailverification.port.in.SendVerificationEmailUseCase;
import com.libreuml.backend.application.passwordreset.port.in.RequestPasswordResetUseCase;
import com.libreuml.backend.application.passwordreset.port.in.ResetPasswordUseCase;
import com.libreuml.backend.application.user.port.in.CreateUserUseCase;
import com.libreuml.backend.infrastructure.in.web.dto.request.auth.ConfirmEmailRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.auth.ForgotPasswordRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.auth.LoginRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.auth.RegisterRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.auth.ResetPasswordRequest;
import com.libreuml.backend.infrastructure.in.web.mapper.AuthWebMapper;
import com.libreuml.backend.infrastructure.security.CustomUserDetails;
import com.libreuml.backend.infrastructure.security.cookie.CookieTokenStrategy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CreateUserUseCase createUserUseCase;
    private final LoginWithRefreshUseCase loginWithRefreshUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final AuthWebMapper authWebMapper;
    private final CookieTokenStrategy cookieTokenStrategy;
    private final SendVerificationEmailUseCase sendVerificationEmailUseCase;
    private final ConfirmEmailUseCase confirmEmailUseCase;
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequest request) {
        var user = createUserUseCase.create(authWebMapper.toCreateCommand(request));
        sendVerificationEmailUseCase.send(user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(
            @RequestBody @Valid LoginRequest body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        var command = authWebMapper.toLoginCommand(body);
        TokenPair tokens = loginWithRefreshUseCase.login(command, resolveClientIp(request), request.getHeader("User-Agent"));

        cookieTokenStrategy.setAccessTokenCookie(response, tokens.accessToken());
        cookieTokenStrategy.setRefreshTokenCookie(response, tokens.rawRefreshToken());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = cookieTokenStrategy.extractRefreshTokenFromCookie(request);
        if (rawRefreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var command = new RefreshCommand(rawRefreshToken, resolveClientIp(request), request.getHeader("User-Agent"));
        TokenPair tokens = refreshTokenUseCase.refresh(command);

        cookieTokenStrategy.setAccessTokenCookie(response, tokens.accessToken());
        cookieTokenStrategy.setRefreshTokenCookie(response, tokens.rawRefreshToken());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = cookieTokenStrategy.extractRefreshTokenFromCookie(request);
        if (rawRefreshToken != null) {
            refreshTokenUseCase.revoke(rawRefreshToken);
        }
        cookieTokenStrategy.clearTokenCookies(response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email/verify/send")
    public ResponseEntity<Void> sendVerificationEmail(
            @AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        sendVerificationEmailUseCase.send(principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email/verify/confirm")
    public ResponseEntity<Void> confirmEmail(
            @RequestBody @Valid ConfirmEmailRequest request) {
        confirmEmailUseCase.confirm(request.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        requestPasswordResetUseCase.request(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        resetPasswordUseCase.reset(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
