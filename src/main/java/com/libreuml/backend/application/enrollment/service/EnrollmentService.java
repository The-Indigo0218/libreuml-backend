package com.libreuml.backend.application.enrollment.service;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.application.enrollment.exception.EnrollmentAlreadyExistsException;
import com.libreuml.backend.application.enrollment.exception.EnrollmentNotFoundException;
import com.libreuml.backend.application.enrollment.port.in.ManageEnrollmentUseCase;
import com.libreuml.backend.application.enrollment.port.in.dto.EnrollmentCommand;
import com.libreuml.backend.application.enrollment.port.out.EnrollmentRepository;
import com.libreuml.backend.domain.model.Enrollment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnrollmentService implements ManageEnrollmentUseCase {

    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional
    public void joinCourse(EnrollmentCommand command) {
        enrollmentRepository.findByStudentIdAndCourseId(command.studentId(), command.courseId())
                .ifPresentOrElse(
                        existingEnrollment -> {
                            if (existingEnrollment.isActive()) {
                                throw new EnrollmentAlreadyExistsException("Student is already enrolled in this course.");
                            }
                            existingEnrollment.activate();
                            enrollmentRepository.save(existingEnrollment);
                        },
                        () -> {
                            Enrollment newEnrollment = Enrollment.builder()
                                    .studentId(command.studentId())
                                    .courseId(command.courseId())
                                    .active(true)
                                    .enrolledAt(LocalDateTime.now())
                                    .build();
                            enrollmentRepository.save(newEnrollment);
                        }
                );
    }

    @Override
    @Transactional
    public void leaveCourse(EnrollmentCommand command) {
        Enrollment enrollment = getEnrollmentDetails(command);
        enrollment.deactivate();
        enrollmentRepository.save(enrollment);
    }

    @Override
    public Enrollment getEnrollmentDetails(EnrollmentCommand command) {
        return enrollmentRepository.findByStudentIdAndCourseId(command.studentId(), command.courseId())
                .orElseThrow(() -> new EnrollmentNotFoundException("Student is not enrolled in this course."));
    }

    public PagedResult<Enrollment> getEnrollmentsByCourseId(UUID courseId, PaginationCommand pagination) {
        return enrollmentRepository.findAllByCourseId(courseId, pagination);
    }

    public PagedResult<Enrollment> getEnrollmentsByStudentId(UUID studentId, PaginationCommand pagination) {
        return enrollmentRepository.findAllByStudentId(studentId, pagination);
    }

}