package com.libreuml.backend.application.report.port.in.dto;

import com.libreuml.backend.domain.model.ReportStatus;

import java.util.UUID;

public record ResponseAndUpdateStatusCommand(UUID id, String adminResponse, String internalNotes, ReportStatus status, UUID responderId) {}