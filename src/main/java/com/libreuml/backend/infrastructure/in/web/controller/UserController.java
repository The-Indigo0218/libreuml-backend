package com.libreuml.backend.infrastructure.in.web.controller;

import com.libreuml.backend.application.user.port.in.dto.ChangePasswordCommand;
import com.libreuml.backend.application.user.port.in.dto.UpdateEmailCommand;
import com.libreuml.backend.application.user.port.in.dto.UpdateSocialProfileCommand;
import com.libreuml.backend.application.user.port.in.dto.UpdateUserBasicInfoCommand;
import com.libreuml.backend.application.user.port.service.UserService;
import com.libreuml.backend.infrastructure.in.web.dto.request.user.UpdateEmailRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.user.UpdatePasswordRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.user.UpdateUserBasicInfoRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.user.UpdaterSocialProfileRequest;
import com.libreuml.backend.infrastructure.in.web.dto.response.user.UserResponse;
import com.libreuml.backend.infrastructure.in.web.mapper.UserWebMapper;
import com.libreuml.backend.infrastructure.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final UserWebMapper userWebMapper;

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
}
