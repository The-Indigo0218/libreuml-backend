package com.libreuml.backend.application.course.service;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.application.courses.exception.CourseAlreadyExistsException;
import com.libreuml.backend.application.courses.exception.CourseNotFoundException;
import com.libreuml.backend.application.courses.port.in.dto.*;
import com.libreuml.backend.application.courses.port.mapper.CourseMapper;
import com.libreuml.backend.application.courses.port.out.CourseRepository;
import com.libreuml.backend.application.courses.port.service.CourseService;
import com.libreuml.backend.application.enrollment.port.out.EnrollmentRepository;
import com.libreuml.backend.application.user.exception.UserNotFoundException;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.*;
import com.libreuml.backend.domain.model.exception.UserNotAuthorizedException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;
    @Mock private CourseMapper courseMapper;
    @Mock private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private CourseService courseService;

    private UUID teacherId;
    private UUID studentId;
    private UUID adminId;
    private User teacher;
    private User student;
    private User admin;
    private Course publicCourse;
    private Course privateCourse;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        adminId   = UUID.randomUUID();

        teacher = Teacher.builder().id(teacherId).role(RoleEnum.TEACHER).build();
        student = Student.builder().id(studentId).role(RoleEnum.STUDENT).build();
        admin   = Teacher.builder().id(adminId).role(RoleEnum.ADMIN).build();

        publicCourse = Course.builder()
                .id(UUID.randomUUID())
                .code("PUB-101")
                .creatorId(teacherId)
                .visibility(VisibilityCourseEnum.PUBLIC)
                .active(true)
                .build();

        privateCourse = Course.builder()
                .id(UUID.randomUUID())
                .code("PRI-101")
                .creatorId(teacherId)
                .visibility(VisibilityCourseEnum.PRIVATE)
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("createCourse")
    class CreateCourse {

        @Test
        @DisplayName("Should create course when teacher provides unique code")
        void success() {
            CreateCourseCommand command = new CreateCourseCommand(
                    "Java Basics", "Intro", teacherId, VisibilityCourseEnum.PUBLIC, "JAVA-101", List.of());
            Course expected = Course.builder().code("JAVA-101").creatorId(teacherId).build();

            when(userRepository.getUserById(teacherId)).thenReturn(Optional.of(teacher));
            when(courseRepository.existsByCode("JAVA-101")).thenReturn(false);
            when(courseRepository.existsBySlug(any())).thenReturn(false);
            when(courseMapper.toDomain(command)).thenReturn(expected);
            when(courseRepository.save(any())).thenReturn(expected);

            Course result = courseService.createCourse(command);

            assertNotNull(result);
            assertEquals("JAVA-101", result.getCode());
            verify(courseRepository).save(any());
        }

        @Test
        @DisplayName("Should throw when student tries to create a course")
        void fail_studentCannotCreate() {
            CreateCourseCommand command = new CreateCourseCommand(
                    "Java Basics", "Intro", studentId, VisibilityCourseEnum.PUBLIC, "JAVA-101", List.of());

            when(userRepository.getUserById(studentId)).thenReturn(Optional.of(student));

            assertThrows(UserNotAuthorizedException.class, () -> courseService.createCourse(command));
            verify(courseRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when course code already exists")
        void fail_duplicateCode() {
            CreateCourseCommand command = new CreateCourseCommand(
                    "Java Basics", "Intro", teacherId, VisibilityCourseEnum.PUBLIC, "JAVA-101", List.of());

            when(userRepository.getUserById(teacherId)).thenReturn(Optional.of(teacher));
            when(courseRepository.existsByCode("JAVA-101")).thenReturn(true);

            assertThrows(CourseAlreadyExistsException.class, () -> courseService.createCourse(command));
            verify(courseRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when more than 5 tags are provided")
        void fail_tooManyTags() {
            CreateCourseCommand command = new CreateCourseCommand(
                    "Java Basics", "Intro", teacherId, VisibilityCourseEnum.PUBLIC, "JAVA-101",
                    List.of("a", "b", "c", "d", "e", "f"));

            assertThrows(IllegalArgumentException.class, () -> courseService.createCourse(command));
            verify(courseRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when creator user does not exist")
        void fail_userNotFound() {
            CreateCourseCommand command = new CreateCourseCommand(
                    "Java Basics", "Intro", teacherId, VisibilityCourseEnum.PUBLIC, "JAVA-101", List.of());

            when(userRepository.getUserById(teacherId)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> courseService.createCourse(command));
        }
    }

    @Nested
    @DisplayName("getCourseById")
    class GetCourseById {

        @Test
        @DisplayName("Should return public course for any authenticated user")
        void success_publicCourse() {
            when(courseRepository.findById(publicCourse.getId())).thenReturn(Optional.of(publicCourse));

            Course result = courseService.getCourseById(publicCourse.getId(), studentId);

            assertNotNull(result);
            assertEquals(VisibilityCourseEnum.PUBLIC, result.getVisibility());
        }

        @Test
        @DisplayName("Should return private course for its creator")
        void success_privateCourse_creator() {
            when(courseRepository.findById(privateCourse.getId())).thenReturn(Optional.of(privateCourse));

            Course result = courseService.getCourseById(privateCourse.getId(), teacherId);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should return private course for enrolled student")
        void success_privateCourse_enrolledStudent() {
            when(courseRepository.findById(privateCourse.getId())).thenReturn(Optional.of(privateCourse));
            when(enrollmentRepository.existsByStudentIdAndCourseId(studentId, privateCourse.getId())).thenReturn(true);

            Course result = courseService.getCourseById(privateCourse.getId(), studentId);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw when non-member accesses private course")
        void fail_privateCourse_nonMember() {
            UUID strangerId = UUID.randomUUID();
            when(courseRepository.findById(privateCourse.getId())).thenReturn(Optional.of(privateCourse));
            when(enrollmentRepository.existsByStudentIdAndCourseId(strangerId, privateCourse.getId())).thenReturn(false);

            assertThrows(UserNotAuthorizedException.class,
                    () -> courseService.getCourseById(privateCourse.getId(), strangerId));
        }

        @Test
        @DisplayName("Should throw when course does not exist")
        void fail_courseNotFound() {
            UUID missingId = UUID.randomUUID();
            when(courseRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThrows(CourseNotFoundException.class,
                    () -> courseService.getCourseById(missingId, studentId));
        }
    }

    @Nested
    @DisplayName("getAllPublicCourses")
    class GetAllPublicCourses {

        @Test
        @DisplayName("Should return paged result from repository")
        void success() {
            PaginationCommand pagination = new PaginationCommand(0, 10, "createdAt", "DESC");
            PagedResult<Course> expected = new PagedResult<>(List.of(publicCourse), 0, 10, 1, 1, true);
            when(courseRepository.findAllPublicCourses(pagination)).thenReturn(expected);

            PagedResult<Course> result = courseService.getAllPublicCourses(pagination);

            assertEquals(1, result.totalElements());
            assertEquals(publicCourse, result.content().get(0));
        }
    }

    @Nested
    @DisplayName("searchCoursesByTitle")
    class SearchCoursesByTitle {

        @Test
        @DisplayName("Should return matching courses paged")
        void success() {
            PaginationCommand pagination = new PaginationCommand(0, 10, "createdAt", "DESC");
            SearchCoursesByTitleCommand command = new SearchCoursesByTitleCommand("Java", pagination);
            PagedResult<Course> expected = new PagedResult<>(List.of(publicCourse), 0, 10, 1, 1, true);
            when(courseRepository.searchByTitle("Java", pagination)).thenReturn(expected);

            PagedResult<Course> result = courseService.searchCursesByTitle(command);

            assertEquals(1, result.content().size());
        }
    }

    @Nested
    @DisplayName("getCoursesByTag")
    class GetCoursesByTag {

        @Test
        @DisplayName("Should return courses matching tag paged")
        void success() {
            PaginationCommand pagination = new PaginationCommand(0, 10, "createdAt", "DESC");
            GetCourseByTag command = new GetCourseByTag("java", pagination);
            PagedResult<Course> expected = new PagedResult<>(List.of(publicCourse), 0, 10, 1, 1, true);
            when(courseRepository.findByTag("java", pagination)).thenReturn(expected);

            PagedResult<Course> result = courseService.getCoursesByTag(command);

            assertEquals(1, result.content().size());
        }
    }

    @Nested
    @DisplayName("getCourseBySlug")
    class GetCourseBySlug {

        @Test
        @DisplayName("Should return course when slug exists")
        void success() {
            publicCourse.assignSlug("java-basics");
            when(courseRepository.findBySlug("java-basics")).thenReturn(Optional.of(publicCourse));

            Course result = courseService.getCourseBySlug("java-basics");

            assertEquals("java-basics", result.getSlug());
        }

        @Test
        @DisplayName("Should throw when slug does not exist")
        void fail_notFound() {
            when(courseRepository.findBySlug("missing-slug")).thenReturn(Optional.empty());

            assertThrows(CourseNotFoundException.class,
                    () -> courseService.getCourseBySlug("missing-slug"));
        }
    }

    @Nested
    @DisplayName("updateTitleAndDescription")
    class UpdateTitleAndDescription {

        @Test
        @DisplayName("Should update when requester is the creator")
        void success() {
            UpdateTitleAndDescriptionCourseCommand command =
                    new UpdateTitleAndDescriptionCourseCommand(publicCourse.getId(), "New Title", "New Desc", teacherId);

            when(userRepository.getUserById(teacherId)).thenReturn(Optional.of(teacher));
            when(courseRepository.findById(publicCourse.getId())).thenReturn(Optional.of(publicCourse));
            when(courseRepository.save(any())).thenReturn(publicCourse);
            doNothing().when(courseMapper).updateTitleAndDescriptionFromCommand(any(), any());

            Course result = courseService.updateTitleAndDescription(command);

            assertNotNull(result);
            verify(courseRepository).save(any());
        }

        @Test
        @DisplayName("Should throw when requester is not the creator")
        void fail_notCreator() {
            UpdateTitleAndDescriptionCourseCommand command =
                    new UpdateTitleAndDescriptionCourseCommand(publicCourse.getId(), "New Title", "New Desc", studentId);

            when(userRepository.getUserById(studentId)).thenReturn(Optional.of(student));
            when(courseRepository.findById(publicCourse.getId())).thenReturn(Optional.of(publicCourse));

            assertThrows(UserNotAuthorizedException.class,
                    () -> courseService.updateTitleAndDescription(command));
            verify(courseRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deactivateCourse")
    class DeactivateCourse {

        @Test
        @DisplayName("Should deactivate when requester is the creator")
        void success_creator() {
            when(courseRepository.findById(publicCourse.getId())).thenReturn(Optional.of(publicCourse));
            when(userRepository.getUserById(teacherId)).thenReturn(Optional.of(teacher));
            when(courseRepository.save(any())).thenReturn(publicCourse);

            courseService.deactivateCourse(new DeactivateCourseCommand(publicCourse.getId(), teacherId));

            assertFalse(publicCourse.getActive());
            verify(courseRepository).save(any());
        }

        @Test
        @DisplayName("Should deactivate when requester is admin")
        void success_admin() {
            when(courseRepository.findById(publicCourse.getId())).thenReturn(Optional.of(publicCourse));
            when(userRepository.getUserById(adminId)).thenReturn(Optional.of(admin));
            when(courseRepository.save(any())).thenReturn(publicCourse);

            courseService.deactivateCourse(new DeactivateCourseCommand(publicCourse.getId(), adminId));

            assertFalse(publicCourse.getActive());
        }

        @Test
        @DisplayName("Should throw when requester is neither creator nor admin")
        void fail_unauthorized() {
            when(courseRepository.findById(publicCourse.getId())).thenReturn(Optional.of(publicCourse));
            when(userRepository.getUserById(studentId)).thenReturn(Optional.of(student));

            assertThrows(UserNotAuthorizedException.class,
                    () -> courseService.deactivateCourse(new DeactivateCourseCommand(publicCourse.getId(), studentId)));
            verify(courseRepository, never()).save(any());
        }
    }
}
