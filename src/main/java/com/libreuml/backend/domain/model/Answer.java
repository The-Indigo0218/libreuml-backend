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
public class Answer {
    private UUID id;
    private String content;
    private Boolean isAccepted;
    private LocalDateTime createdAt;
    private List<String> imageUrls;
    private boolean active;
    private UUID creatorId;
    private UUID questionId;

    public void accept() {
        if (isAccepted) {
            return;
        }
        this.isAccepted = true;
    }

    public void deactivate(User user) {
        if (isNotAuthorized(user)) {
            throw new UserNotAuthorizedException("User not authorized to deactivate this answer");
        }
        this.active = false;
    }

    private boolean isNotAuthorized(User user) {
        boolean isOwner = this.creatorId.equals(user.getId());
        boolean isModerator = RoleEnum.MODERATOR.equals(user.getRole());
        boolean isAdmin = RoleEnum.ADMIN.equals(user.getRole());
        return !isOwner && !isModerator && !isAdmin;
    }


}