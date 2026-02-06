package com.libreuml.backend.infrastructure.in.web.controller;

import com.libreuml.backend.application.user.port.in.CreateUserUseCase;
import com.libreuml.backend.application.user.port.in.LoginUseCase;
import com.libreuml.backend.infrastructure.in.web.dto.AuthResponse;
import com.libreuml.backend.infrastructure.in.web.dto.LoginRequest;
import com.libreuml.backend.infrastructure.in.web.dto.RegisterRequest;
import com.libreuml.backend.infrastructure.in.web.mapper.AuthWebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CreateUserUseCase createUserUseCase;
    private final LoginUseCase loginUseCase;
    private final AuthWebMapper authWebMapper;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequest request) {
        var command = authWebMapper.toCreateCommand(request);
        createUserUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        var command = authWebMapper.toLoginCommand(request);
        String token = loginUseCase.login(command);

        return ResponseEntity.ok(new AuthResponse(token));
    }
}