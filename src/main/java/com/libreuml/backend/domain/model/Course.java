package com.libreuml.backend.domain.model;

import com.libreuml.backend.application.exception.UserNotAuthorizedException;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Course {
    private UUID id;
    private String code;
    private String title;
    private String description;
    private Boolean active;
    private UUID creatorId;
    private LocalDateTime createdAt;
    private String coverUrl;
    private VisibilityCourseEnum visibility;

    public void deactivate(User user) {
        if (!canDeactivate(user)) {
            throw new UserNotAuthorizedException("User is not authorized to deactivate this course");
        }
        this.active = false;

    }

    private boolean canDeactivate(User user) {
        return user.getId().equals(this.creatorId) || user.getRole().equals(RoleEnum.ADMIN);
    }
}