package com.libreuml.backend.application.enrollment.service;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.application.enrollment.exception.EnrollmentAlreadyExistsException;
import com.libreuml.backend.application.enrollment.exception.EnrollmentNotFoundException;
import com.libreuml.backend.application.enrollment.port.in.dto.EnrollmentCommand;
import com.libreuml.backend.application.enrollment.port.out.EnrollmentRepository;
import com.libreuml.backend.application.enrollment.port.service.EnrollmentService;
import com.libreuml.backend.domain.model.Enrollment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private UUID studentId;
    private UUID courseId;
    private EnrollmentCommand command;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        courseId  = UUID.randomUUID();
        command   = new EnrollmentCommand(studentId, courseId);
    }

    @Nested
    @DisplayName("joinCourse")
    class JoinCourse {

        @Test
        @DisplayName("Should create new enrollment when student has never enrolled")
        void success_newEnrollment() {
            when(enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId))
                    .thenReturn(Optional.empty());

            enrollmentService.joinCourse(command);

            ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
            verify(enrollmentRepository).save(captor.capture());
            Enrollment saved = captor.getValue();
            assertEquals(studentId, saved.getStudentId());
            assertEquals(courseId, saved.getCourseId());
            assertTrue(saved.isActive());
            assertNotNull(saved.getEnrolledAt());
        }

        @Test
        @DisplayName("Should reactivate enrollment when student previously left the course")
        void success_reactivateInactiveEnrollment() {
            Enrollment inactive = Enrollment.builder()
                    .studentId(studentId)
                    .courseId(courseId)
                    .active(false)
                    .enrolledAt(LocalDateTime.now().minusDays(10))
                    .build();

            when(enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId))
                    .thenReturn(Optional.of(inactive));

            enrollmentService.joinCourse(command);

            assertTrue(inactive.isActive());
            verify(enrollmentRepository).save(inactive);
        }

        @Test
        @DisplayName("Should throw when student is already actively enrolled")
        void fail_alreadyEnrolled() {
            Enrollment active = Enrollment.builder()
                    .studentId(studentId)
                    .courseId(courseId)
                    .active(true)
                    .enrolledAt(LocalDateTime.now())
                    .build();

            when(enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId))
                    .thenReturn(Optional.of(active));

            assertThrows(EnrollmentAlreadyExistsException.class,
                    () -> enrollmentService.joinCourse(command));
            verify(enrollmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("leaveCourse")
    class LeaveCourse {

        @Test
        @DisplayName("Should deactivate enrollment when student leaves")
        void success() {
            Enrollment active = Enrollment.builder()
                    .studentId(studentId)
                    .courseId(courseId)
                    .active(true)
                    .enrolledAt(LocalDateTime.now())
                    .build();

            when(enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId))
                    .thenReturn(Optional.of(active));

            enrollmentService.leaveCourse(command);

            assertFalse(active.isActive());
            verify(enrollmentRepository).save(active);
        }

        @Test
        @DisplayName("Should throw when enrollment does not exist")
        void fail_notEnrolled() {
            when(enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId))
                    .thenReturn(Optional.empty());

            assertThrows(EnrollmentNotFoundException.class,
                    () -> enrollmentService.leaveCourse(command));
            verify(enrollmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getEnrollmentDetails")
    class GetEnrollmentDetails {

        @Test
        @DisplayName("Should return enrollment when it exists")
        void success() {
            Enrollment enrollment = Enrollment.builder()
                    .studentId(studentId)
                    .courseId(courseId)
                    .active(true)
                    .build();

            when(enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId))
                    .thenReturn(Optional.of(enrollment));

            Enrollment result = enrollmentService.getEnrollmentDetails(command);

            assertNotNull(result);
            assertEquals(studentId, result.getStudentId());
        }

        @Test
        @DisplayName("Should throw when enrollment does not exist")
        void fail_notFound() {
            when(enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId))
                    .thenReturn(Optional.empty());

            assertThrows(EnrollmentNotFoundException.class,
                    () -> enrollmentService.getEnrollmentDetails(command));
        }
    }

    @Nested
    @DisplayName("getEnrollmentsByCourseId")
    class GetEnrollmentsByCourseId {

        @Test
        @DisplayName("Should return paged enrollments for a course")
        void success() {
            PaginationCommand pagination = new PaginationCommand(0, 10, "createdAt", "DESC");
            Enrollment enrollment = Enrollment.builder().studentId(studentId).courseId(courseId).build();
            PagedResult<Enrollment> expected = new PagedResult<>(List.of(enrollment), 0, 10, 1, 1, true);

            when(enrollmentRepository.findAllByCourseId(courseId, pagination)).thenReturn(expected);

            PagedResult<Enrollment> result = enrollmentService.getEnrollmentsByCourseId(courseId, pagination);

            assertEquals(1, result.totalElements());
            assertEquals(studentId, result.content().get(0).getStudentId());
        }
    }

    @Nested
    @DisplayName("getEnrollmentsByStudentId")
    class GetEnrollmentsByStudentId {

        @Test
        @DisplayName("Should return paged enrollments for a student")
        void success() {
            PaginationCommand pagination = new PaginationCommand(0, 10, "createdAt", "DESC");
            Enrollment enrollment = Enrollment.builder().studentId(studentId).courseId(courseId).build();
            PagedResult<Enrollment> expected = new PagedResult<>(List.of(enrollment), 0, 10, 1, 1, true);

            when(enrollmentRepository.findAllByStudentId(studentId, pagination)).thenReturn(expected);

            PagedResult<Enrollment> result = enrollmentService.getEnrollmentsByStudentId(studentId, pagination);

            assertEquals(1, result.totalElements());
            assertEquals(courseId, result.content().get(0).getCourseId());
        }
    }
}
