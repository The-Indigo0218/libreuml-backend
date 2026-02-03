package com.libreuml.backend.application.user.port.in;

import com.libreuml.backend.application.user.port.in.dto.*;
import com.libreuml.backend.domain.model.User;

import java.util.UUID;

public interface UpdateUserUseCase {
    User updateUserSocialProfile(UpdateSocialProfileCommand command);
    User updateUserProfilePicture(UpdateProfilePictureCommand command);
    User updateUserEmail(UpdateEmailCommand command);
    User updateUserBasicInfo(UpdateUserBasicInfoCommand command);
    User updateUserPassword(ChangePasswordCommand command);
    User deactivateUser(DeactivateUserCommand command);
}
