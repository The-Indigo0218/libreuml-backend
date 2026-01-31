package com.libreuml.backend.infrastructure.in.rest;

import com.libreuml.backend.application.port.in.dto.LoginCommand;
import com.libreuml.backend.application.port.in.LoginUseCase;
import com.libreuml.backend.infrastructure.in.rest.dto.LoginRequest;
import com.libreuml.backend.infrastructure.in.rest.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth") // Ruta base separada
@RequiredArgsConstructor
public class AuthRestController {

    private final LoginUseCase loginUseCase;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        var command = LoginCommand.builder()
                .email(request.email())
                .password(request.password())
                .build();

        String token = loginUseCase.login(command);
        return ResponseEntity.ok(new TokenResponse(token));
    }
}