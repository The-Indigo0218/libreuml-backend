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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
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
        return courseRepository.findBySlug(slug).map(courseMapper::toDomain);
    }

    @Override
    public long countAll() {
        return courseRepository.count();
    }

    @Override
    public long countAllPublic() {
        return courseRepository.countByVisibility(VisibilityCourseEnum.PUBLIC);
    }

    @Override
    public PagedResult<Course> findAllPublicCourses(PaginationCommand pagination) {
        Page<CourseEntity> page = courseRepository.findAllByVisibility(VisibilityCourseEnum.PUBLIC, toPageable(pagination));
        return toPagedResult(page);
    }

    @Override
    public PagedResult<Course> findAllByCreatorId(UUID creatorId, PaginationCommand pagination) {
        Page<CourseEntity> page = courseRepository.findAllByCreatorId(creatorId, toPageable(pagination));
        return toPagedResult(page);
    }

    @Override
    public PagedResult<Course> searchByTitle(String title, PaginationCommand pagination) {
        Page<CourseEntity> page = courseRepository.findByTitleContainingIgnoreCase(title, toPageable(pagination));
        return toPagedResult(page);
    }

    @Override
    public PagedResult<Course> findByTag(String tag, PaginationCommand pagination) {
        Page<CourseEntity> page = courseRepository.findByTags(tag, toPageable(pagination));
        return toPagedResult(page);
    }

    private org.springframework.data.domain.Pageable toPageable(PaginationCommand pagination) {
        Sort.Direction dir = "ASC".equalsIgnoreCase(pagination.direction()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(pagination.page(), pagination.size(), Sort.by(dir, pagination.sortBy()));
    }

    private PagedResult<Course> toPagedResult(Page<CourseEntity> page) {
        List<Course> content = page.getContent().stream().map(courseMapper::toDomain).toList();
        return new PagedResult<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }
}