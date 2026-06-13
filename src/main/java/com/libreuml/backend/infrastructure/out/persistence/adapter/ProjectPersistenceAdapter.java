package com.libreuml.backend.infrastructure.out.persistence.adapter;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.project.port.out.ProjectRepository;
import com.libreuml.backend.domain.model.Project;
import com.libreuml.backend.infrastructure.out.persistence.entity.ProjectEntity;
import com.libreuml.backend.infrastructure.out.persistence.repository.SpringDataProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter for {@link Project} aggregates. Mapping is manual (no MapStruct) because
 * {@code vfsSnapshot} is a Jackson {@code ObjectNode}; the rationale matches
 * {@link DiagramPersistenceAdapter}, including the {@code saveAndFlush} that forces the
 * {@code @Version} increment before the method returns.
 */
@Component
@RequiredArgsConstructor
public class ProjectPersistenceAdapter implements ProjectRepository {

    private final SpringDataProjectRepository jpaRepository;

    @Override
    public Project save(Project project) {
        ProjectEntity saved = jpaRepository.saveAndFlush(toEntity(project));
        return toDomain(saved);
    }

    @Override
    public Optional<Project> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public PagedResult<Project> findAllByOwnerId(UUID ownerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<ProjectEntity> projectPage = jpaRepository.findAllByOwnerId(ownerId, pageable);
        return new PagedResult<>(
                projectPage.getContent().stream().map(this::toDomain).toList(),
                projectPage.getNumber(),
                projectPage.getSize(),
                projectPage.getTotalElements(),
                projectPage.getTotalPages(),
                projectPage.isLast()
        );
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private ProjectEntity toEntity(Project project) {
        return ProjectEntity.builder()
                .id(project.getId())
                .ownerId(project.getOwnerId())
                .name(project.getName())
                .description(project.getDescription())
                .author(project.getAuthor())
                .projectVersion(project.getProjectVersion())
                .projectKind(project.getProjectKind())
                .targetLanguage(project.getTargetLanguage())
                .basePackage(project.getBasePackage())
                .visibility(project.getVisibility())
                .vfsSnapshot(project.getVfsSnapshot())
                .version(project.getVersion())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    private Project toDomain(ProjectEntity entity) {
        return Project.builder()
                .id(entity.getId())
                .ownerId(entity.getOwnerId())
                .name(entity.getName())
                .description(entity.getDescription())
                .author(entity.getAuthor())
                .projectVersion(entity.getProjectVersion())
                .projectKind(entity.getProjectKind())
                .targetLanguage(entity.getTargetLanguage())
                .basePackage(entity.getBasePackage())
                .visibility(entity.getVisibility())
                .vfsSnapshot(entity.getVfsSnapshot())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
