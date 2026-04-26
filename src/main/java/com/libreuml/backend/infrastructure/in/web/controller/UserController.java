package com.libreuml.backend.infrastructure.in.web.controller;

import com.libreuml.backend.application.auth.dto.OAuthProvider;
import com.libreuml.backend.application.auth.port.in.SessionUseCase;
import com.libreuml.backend.application.user.port.in.DeleteAccountUseCase;
import com.libreuml.backend.application.user.port.in.UnlinkOAuthUseCase;
import com.libreuml.backend.application.user.port.in.dto.ChangePasswordCommand;
import com.libreuml.backend.application.user.port.in.dto.UpdateEmailCommand;
import com.libreuml.backend.application.user.port.in.dto.UpdateSocialProfileCommand;
import com.libreuml.backend.application.user.port.in.dto.UpdateUserBasicInfoCommand;
import com.libreuml.backend.application.user.port.service.UserService;
import com.libreuml.backend.infrastructure.in.web.dto.request.user.UpdateEmailRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.user.UpdatePasswordRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.user.UpdateUserBasicInfoRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.user.UpdaterSocialProfileRequest;
import com.libreuml.backend.infrastructure.in.web.dto.response.auth.SessionResponse;
import com.libreuml.backend.infrastructure.in.web.dto.response.user.UserResponse;
import com.libreuml.backend.infrastructure.in.web.mapper.UserWebMapper;
import com.libreuml.backend.infrastructure.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final UserWebMapper userWebMapper;
    private final SessionUseCase sessionUseCase;
    private final DeleteAccountUseCase deleteAccountUseCase;
    private final UnlinkOAuthUseCase unlinkOAuthUseCase;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(userWebMapper.toUserResponse(userService.getUserById(userDetails.getId())));
    }

    @PatchMapping("/about_me")
    public ResponseEntity<UserResponse> updateUserBasicInfo(
            @RequestBody @Valid UpdateUserBasicInfoRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UpdateUserBasicInfoCommand command = userWebMapper.toUpdateUserBasicInfoCommand(request, userDetails.getId());
        UserResponse response = userWebMapper.toUserResponse(userService.updateUserBasicInfo(command));
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/social_profile")
    public ResponseEntity<UserResponse> updateSocialProfile(
            @RequestBody @Valid UpdaterSocialProfileRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UpdateSocialProfileCommand command = userWebMapper.toUpdateSocialProfileCommand(request, userDetails.getId());
        UserResponse response = userWebMapper.toUserResponse(userService.updateUserSocialProfile(command));
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/password")
    public ResponseEntity<UserResponse> updatePassword(
           @RequestBody @Valid UpdatePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ChangePasswordCommand command = userWebMapper.toChangePasswordCommand(request, userDetails.getId());
        UserResponse response = userWebMapper.toUserResponse(userService.updateUserPassword(command));
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/email")
    public ResponseEntity<UserResponse> updateEmail(
            @RequestBody @Valid UpdateEmailRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UpdateEmailCommand command = userWebMapper.toUpdateEmailCommand(request, userDetails.getId());
        UserResponse response = userWebMapper.toUserResponse(userService.updateUserEmail(command));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/sessions")
    public ResponseEntity<List<SessionResponse>> getSessions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<SessionResponse> sessions = sessionUseCase.listSessions(userDetails.getId())
                .stream().map(SessionResponse::from).toList();
        return ResponseEntity.ok(sessions);
    }

    @DeleteMapping("/me/sessions/{id}")
    public ResponseEntity<Void> revokeSession(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        sessionUseCase.revokeSession(id, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        deleteAccountUseCase.deleteAccount(userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me/oauth/{provider}")
    public ResponseEntity<Void> unlinkOAuth(
            @PathVariable String provider,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        OAuthProvider oAuthProvider;
        try {
            oAuthProvider = OAuthProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        unlinkOAuthUseCase.unlinkOAuth(userDetails.getId(), oAuthProvider);
        return ResponseEntity.noContent().build();
    }
}
