package com.libreuml.backend.infrastructure.in.web.mapper;

import com.libreuml.backend.application.user.port.in.dto.ChangePasswordCommand;
import com.libreuml.backend.application.user.port.in.dto.UpdateEmailCommand;
import com.libreuml.backend.application.user.port.in.dto.UpdateSocialProfileCommand;
import com.libreuml.backend.application.user.port.in.dto.UpdateUserBasicInfoCommand;
import com.libreuml.backend.domain.model.RoleEnum;
import com.libreuml.backend.domain.model.User;
import com.libreuml.backend.infrastructure.in.web.dto.request.user.UpdateEmailRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.user.UpdatePasswordRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.user.UpdateUserBasicInfoRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.user.UpdaterSocialProfileRequest;
import com.libreuml.backend.infrastructure.in.web.dto.response.user.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserWebMapper {

    UpdateUserBasicInfoCommand toUpdateUserBasicInfoCommand(UpdateUserBasicInfoRequest request, UUID id);

    UserResponse toUserResponse(User user);

    UpdateSocialProfileCommand toUpdateSocialProfileCommand(UpdaterSocialProfileRequest request, UUID id);

    ChangePasswordCommand toChangePasswordCommand(UpdatePasswordRequest request, UUID id);

    UpdateEmailCommand toUpdateEmailCommand(UpdateEmailRequest request, UUID id);

    @Named("ValidateUserRole")
    default void validateUserRole(String userRole) {
        try {
             RoleEnum roleEnum = RoleEnum.valueOf(userRole);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid User Role. Allowed: USER, TEACHER");
        }
    }
}
