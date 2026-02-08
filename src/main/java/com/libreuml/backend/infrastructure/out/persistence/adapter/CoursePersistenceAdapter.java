package com.libreuml.backend.infrastructure.out.persistence.adapter;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.application.courses.port.out.CourseRepository;
import com.libreuml.backend.domain.model.Course;
import com.libreuml.backend.domain.model.VisibilityCourseEnum;
import com.libreuml.backend.infrastructure.out.persistence.entity.CourseEntity;
import com.libreuml.backend.infrastructure.out.persistence.mapper.CoursePersistenceMapper;
import com.libreuml.backend.infrastructure.out.persistence.repository.SpringDataCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CoursePersistenceAdapter implements CourseRepository {

    private final SpringDataCourseRepository courseRepository;
    private final CoursePersistenceMapper courseMapper;

    @Override
    public Course save(Course course) {
        CourseEntity entity = courseMapper.toEntity(course);
        CourseEntity saved = courseRepository.save(entity);
        return courseMapper.toDomain(saved);
    }

    @Override
    public Optional<Course> findById(UUID id) {
        return courseRepository.findById(id)
                .map(courseMapper::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return courseRepository.existsByCode(code);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return courseRepository.existsBySlug(slug);
    }

    @Override
    public Optional<Course> findBySlug(String slug) {
        return Optional.empty();
    }

    @Override
    public long countAll() {
        return courseRepository.count();
    }

    @Override
    public long countAllPublic() {
        // Here is your specific request! 🎯
        return courseRepository.countByVisibility(VisibilityCourseEnum.PUBLIC);
    }

    // ... Implement the other pagination methods (findAllByCreatorId, etc.)
    // using standard JpaRepository Pageable methods.
    @Override
    public PagedResult<Course> findAllPublicCourses(PaginationCommand pagination) {
        // Placeholder implementation
        return null;
    }

    @Override
    public PagedResult<Course> findAllByCreatorId(UUID creatorId, PaginationCommand pagination) {
        return null;
    }

    @Override
    public PagedResult<Course> searchByTitle(String title, PaginationCommand pagination) {
        return null;
    }

    @Override
    public PagedResult<Course> findByTag(String tag, PaginationCommand pagination) {
        return null;
    }
}