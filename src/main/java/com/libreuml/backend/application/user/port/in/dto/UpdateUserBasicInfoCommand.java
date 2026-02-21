package com.libreuml.backend.application.user.port.in.dto;

import java.util.List;
import java.util.UUID;

public record UpdateUserBasicInfoCommand(UUID id, String fullName, List<String> academicDegrees, List<String> organization, List<String> stacks) {
}
