package com.libreuml.backend.infrastructure.in.web.mapper;

import com.libreuml.backend.application.user.port.in.dto.CreateUserCommand;
import com.libreuml.backend.application.user.port.in.dto.LoginCommand;
import com.libreuml.backend.domain.model.RoleEnum;
import com.libreuml.backend.infrastructure.in.web.dto.request.auth.LoginRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.auth.RegisterRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface AuthWebMapper {

    LoginCommand toLoginCommand(LoginRequest request);

    @Mapping(target = "role", source = "role", qualifiedByName = "mapAndValidateRole")
    CreateUserCommand toCreateCommand(RegisterRequest request);

    @Named("mapAndValidateRole")
    default RoleEnum mapAndValidateRole(String roleStr) {
        try {
            RoleEnum role = RoleEnum.valueOf(roleStr.toUpperCase());
            if (role == RoleEnum.ADMIN || role == RoleEnum.MODERATOR || role == RoleEnum.DEVELOPER) {
                throw new SecurityException("Access Denied: Cannot register with elevated privileges via public API.");
            }
            return role;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Role. Allowed: STUDENT, TEACHER");
        }
    }
}