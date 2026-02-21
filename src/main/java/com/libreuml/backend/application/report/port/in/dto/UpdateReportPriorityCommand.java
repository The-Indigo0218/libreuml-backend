package com.libreuml.backend.application.report.port.in.dto;

import java.util.UUID;

public record UpdateReportPriorityCommand(UUID id, String priority, UUID responderId) {}