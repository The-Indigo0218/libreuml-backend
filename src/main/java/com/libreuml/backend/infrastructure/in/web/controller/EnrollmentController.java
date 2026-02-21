package com.libreuml.backend.infrastructure.in.web.controller;

import com.libreuml.backend.application.enrollment.port.in.ManageEnrollmentUseCase;
import com.libreuml.backend.application.enrollment.port.in.dto.EnrollmentCommand;
import com.libreuml.backend.domain.model.Enrollment;
import com.libreuml.backend.infrastructure.in.web.dto.response.enrollment.EnrollmentResponse;
import com.libreuml.backend.infrastructure.in.web.mapper.EnrollmentWebMapper;
import com.libreuml.backend.infrastructure.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class EnrollmentController {

    private final ManageEnrollmentUseCase manageEnrollmentUseCase;
    private final EnrollmentWebMapper enrollmentWebMapper;


    @PostMapping("/{courseId}/join")
    public ResponseEntity<Void> joinCourse(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        EnrollmentCommand command = enrollmentWebMapper.toCommand(userDetails.getId(), courseId);
        manageEnrollmentUseCase.joinCourse(command);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{courseId}/leave")
    public ResponseEntity<Void> leaveCourse(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        EnrollmentCommand command = enrollmentWebMapper.toCommand(userDetails.getId(), courseId);
        manageEnrollmentUseCase.leaveCourse(command);
        return ResponseEntity.noContent().build();
    }

    // --- Teacher Actions ---

    @GetMapping("/{courseId}/students/{studentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<EnrollmentResponse> getStudentEnrollment(
            @PathVariable UUID courseId,
            @PathVariable UUID studentId
    ) {
        EnrollmentCommand command = enrollmentWebMapper.toCommand(studentId, courseId);
        Enrollment enrollment = manageEnrollmentUseCase.getEnrollmentDetails(command);
        return ResponseEntity.ok(enrollmentWebMapper.toResponse(enrollment));
    }
}