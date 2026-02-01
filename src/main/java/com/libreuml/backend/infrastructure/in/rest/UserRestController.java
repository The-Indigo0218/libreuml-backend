package com.libreuml.backend.infrastructure.in.rest;

import com.libreuml.backend.application.user.port.in.dto.CreateUserCommand;
import com.libreuml.backend.application.user.port.in.CreateUserUseCase;
import com.libreuml.backend.application.user.port.in.GetUserUseCase;
import com.libreuml.backend.domain.model.User;
import com.libreuml.backend.infrastructure.in.rest.dto.RegisterRequest;
import com.libreuml.backend.infrastructure.in.rest.dto.UserWebResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserRestController {

    private final CreateUserUseCase createUserUseCase;
    private final GetUserUseCase getUserUseCase;

    @PostMapping
    public ResponseEntity<UserWebResponse> register(@Valid @RequestBody RegisterRequest request) {
        var command = CreateUserCommand.builder()
                .email(request.email())
                .password(request.password())
                .fullName(request.fullName())
                .role(request.role())
                .build();
        User createdUser = createUserUseCase.create(command);

        UserWebResponse response = toWebResponse(createdUser);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserWebResponse> getUser(@PathVariable UUID id) {
        User user = getUserUseCase.getUserById(id);
        return ResponseEntity.ok(toWebResponse(user));
    }

    private UserWebResponse toWebResponse(User user) {
        return new UserWebResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getActive()
        );
    }
}