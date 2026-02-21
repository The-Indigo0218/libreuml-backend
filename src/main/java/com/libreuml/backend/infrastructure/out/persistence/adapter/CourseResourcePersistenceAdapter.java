package com.libreuml.backend.infrastructure.out.persistence.adapter;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.application.courseResource.port.out.CourseResourceRepository;
import com.libreuml.backend.domain.model.CourseResource;
import com.libreuml.backend.infrastructure.out.persistence.entity.CourseResourceEntity;
import com.libreuml.backend.infrastructure.out.persistence.mapper.CourseResourcePersistenceMapper;
import com.libreuml.backend.infrastructure.out.persistence.repository.SpringDataCourseResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class CourseResourcePersistenceAdapter implements CourseResourceRepository {

    private final CourseResourcePersistenceMapper courseResourcePersistenceMapper;
    private final SpringDataCourseResourceRepository springDataCourseResourceRepository;

    @Override
    public CourseResource save(CourseResource courseResource) {
        CourseResourceEntity entity = courseResourcePersistenceMapper.toEntity(courseResource);
        CourseResourceEntity savedEntity = springDataCourseResourceRepository.save(entity);
        return courseResourcePersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public PagedResult<CourseResource> getAllCourseResourcesByCourseId(UUID courseId, PaginationCommand command) {
       Pageable pageable = toPageable(command);
       Page<CourseResourceEntity> page = springDataCourseResourceRepository.findAllByCourseId(courseId, pageable);
       return toPagedResult(page);
    }

    @Override
    public Optional<CourseResource> findById(UUID id) {
        return springDataCourseResourceRepository.findById(id)
                .map(courseResourcePersistenceMapper::toDomain);
    }

    private Pageable toPageable(PaginationCommand command) {
        return PageRequest.of(command.page(), command.size());
    }

    private PagedResult<CourseResource> toPagedResult(Page<CourseResourceEntity> page) {
        return new PagedResult<>(
                page.getContent().stream()
                        .map(courseResourcePersistenceMapper::toDomain)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

}
