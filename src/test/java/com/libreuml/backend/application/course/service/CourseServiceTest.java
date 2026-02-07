package com.libreuml.backend.application.course.service;

import com.libreuml.backend.application.courses.port.out.CourseRepository;
import com.libreuml.backend.application.courses.port.service.CourseService;
import com.libreuml.backend.application.courses.port.mapper.CourseMapper;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.*;
import com.libreuml.backend.application.courses.port.in.dto.CreateCourseCommand;
import com.libreuml.backend.application.exception.UserNotAuthorizedException;

import org.junit.jupiter.api.DisplayName;
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

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseMapper courseMapper;

    @InjectMocks
    private CourseService courseService;

    @Test
    @DisplayName("Should create course successfully when user is Teacher and code is unique")
    void createCourse_Success() {
        UUID teacherId = UUID.randomUUID();

        User teacher = Teacher.builder()
                .id(teacherId)
                .role(RoleEnum.TEACHER)
                .build();

        CreateCourseCommand command = new CreateCourseCommand(
                "Java Basics",
                "Intro description",
                teacherId,
                VisibilityCourseEnum.PUBLIC,
                "JAVA-101",
                List.of()
        );

        Course expectedCourse = Course.builder()
                .code("JAVA-101")
                .creatorId(teacherId)
                .build();

        when(userRepository.getUserById(teacherId)).thenReturn(Optional.of(teacher));
        when(courseRepository.existsByCode("JAVA-101")).thenReturn(false);
        when(courseMapper.toDomain(command)).thenReturn(expectedCourse);
        when(courseRepository.save(any(Course.class))).thenReturn(expectedCourse);

        Course result = courseService.createCourse(command);

        assertNotNull(result);
        assertEquals("JAVA-101", result.getCode());
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    @DisplayName("Should throw exception when Student tries to create course")
    void createCourse_Fail_Student() {
        UUID studentId = UUID.randomUUID();

        User student = Student.builder()
                .id(studentId)
                .role(RoleEnum.STUDENT)
                .build();

        CreateCourseCommand command = new CreateCourseCommand(
                "Java Basics",
                "Intro description",
                studentId,
                VisibilityCourseEnum.PUBLIC,
                "JAVA-101",
                List.of()
        );

        when(userRepository.getUserById(studentId)).thenReturn(Optional.of(student));

        assertThrows(UserNotAuthorizedException.class, () -> {
            courseService.createCourse(command);
        });

        verify(courseRepository, never()).save(any());
        verify(courseRepository, never()).existsByCode(anyString());
    }
}
