package com.libreuml.backend.application.report.port.in.dto;

import java.util.UUID;

public record UpdateReportStatusCommand(UUID id, String status, UUID responderId) {}