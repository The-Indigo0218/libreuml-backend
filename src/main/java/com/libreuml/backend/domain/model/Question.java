package com.libreuml.backend.domain.model;

import com.libreuml.backend.application.exception.UserNotAuthorizedException;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Question {
    private UUID id;
    private String title;
    private String content;
    private List<String> tags;
    private Boolean active;
    private Boolean isSolved;
    private LocalDateTime createdAt;
    private UUID creatorId;
    private List<String> imageUrls;

    public void deactivate(User user) {
        if (isNotAuthorized(user)) {
            throw new UserNotAuthorizedException("User not authorized to deactivate this question");
        }
        this.active = false;
    }

    public void resolve(User user) {
        if (isNotAuthorized(user)) {
            throw new UserNotAuthorizedException("User not authorized to resolve this question");
        }
        this.isSolved = true;
    }

    private boolean isNotAuthorized(User user) {
        boolean isOwner = this.creatorId.equals(user.getId());
        boolean isModerator = RoleEnum.MODERATOR.equals(user.getRole());
        boolean isAdmin = RoleEnum.ADMIN.equals(user.getRole());

        return !isOwner && !isModerator && !isAdmin;
    }
}