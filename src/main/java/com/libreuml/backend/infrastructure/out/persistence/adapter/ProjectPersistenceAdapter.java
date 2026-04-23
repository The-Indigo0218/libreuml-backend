package com.libreuml.backend.infrastructure.out.persistence.adapter;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.project.dto.ProjectSummary;
import com.libreuml.backend.application.project.port.out.ProjectRepository;
import com.libreuml.backend.domain.model.ApiDiagramType;
import com.libreuml.backend.domain.model.Project;
import com.libreuml.backend.infrastructure.out.persistence.entity.ProjectEntity;
import com.libreuml.backend.infrastructure.out.persistence.repository.SpringDataProjectDiagramRepository;
import com.libreuml.backend.infrastructure.out.persistence.repository.SpringDataProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProjectPersistenceAdapter implements ProjectRepository {

    private final SpringDataProjectRepository jpaRepository;
    private final SpringDataProjectDiagramRepository diagramRepository;

    @Override
    public Project save(Project project) {
        ProjectEntity entity = toEntity(project);
        return toDomain(jpaRepository.saveAndFlush(entity));
    }

    @Override
    public Optional<Project> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public PagedResult<ProjectSummary> findSummariesByOwnerId(UUID ownerId, int page, int size) {
        Page<ProjectEntity> projectPage = jpaRepository.findAllByOwnerId(
                ownerId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));

        List<UUID> projectIds = projectPage.getContent().stream()
                .map(ProjectEntity::getId)
                .toList();

        Map<UUID, List<ApiDiagramType>> typesByProject = new HashMap<>();
        Map<UUID, Long> countByProject = new HashMap<>();

        if (!projectIds.isEmpty()) {
            List<Object[]> rows = diagramRepository.getDiagramTypeCountsByProjectIds(projectIds);
            for (Object[] row : rows) {
                UUID projectId = UUID.fromString((String) row[0]);
                ApiDiagramType type = ApiDiagramType.valueOf((String) row[1]);
                typesByProject.computeIfAbsent(projectId, k -> new ArrayList<>()).add(type);
                countByProject.merge(projectId, ((Number) row[2]).longValue(), Long::sum);
            }
        }

        List<ProjectSummary> summaries = projectPage.getContent().stream()
                .map(e -> new ProjectSummary(
                        e.getId(),
                        e.getName(),
                        e.getDescription(),
                        e.getAuthor(),
                        e.getProjectVersion(),
                        e.getTargetLanguage(),
                        e.getBasePackage(),
                        e.getVisibility(),
                        e.getVersion(),
                        countByProject.getOrDefault(e.getId(), 0L),
                        typesByProject.getOrDefault(e.getId(), List.of()),
                        e.getCreatedAt(),
                        e.getUpdatedAt()))
                .toList();

        return new PagedResult<>(
                summaries,
                projectPage.getNumber(),
                projectPage.getSize(),
                projectPage.getTotalElements(),
                projectPage.getTotalPages(),
                projectPage.isLast());
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void touchUpdatedAt(UUID projectId) {
        jpaRepository.touchUpdatedAt(projectId);
    }

    private ProjectEntity toEntity(Project p) {
        return ProjectEntity.builder()
                .id(p.getId())
                .ownerId(p.getOwnerId())
                .name(p.getName())
                .description(p.getDescription())
                .author(p.getAuthor())
                .projectVersion(p.getProjectVersion())
                .targetLanguage(p.getTargetLanguage())
                .basePackage(p.getBasePackage())
                .visibility(p.getVisibility())
                .version(p.getVersion())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private Project toDomain(ProjectEntity e) {
        return Project.builder()
                .id(e.getId())
                .ownerId(e.getOwnerId())
                .name(e.getName())
                .description(e.getDescription())
                .author(e.getAuthor())
                .projectVersion(e.getProjectVersion())
                .targetLanguage(e.getTargetLanguage())
                .basePackage(e.getBasePackage())
                .visibility(e.getVisibility())
                .version(e.getVersion())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
