package com.libreuml.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Builder
@Setter
@Getter
@AllArgsConstructor
public class Report {
    UUID id;
    UUID userId;
    ReportType type;
    ReportStatus status;
    ReportPriority priority;
    String title;
    String description;
    String adminResponse;
    String internalNotes;
    LocalDateTime createdAt;
    LocalDateTime solvedAt;
    Set<String> evidencesImages;

    public void solveReportTime(){
        this.solvedAt = LocalDateTime.now();
    }
}
