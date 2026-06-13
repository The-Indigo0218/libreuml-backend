package com.libreuml.backend.infrastructure.out.persistence.adapter;

import com.libreuml.backend.application.project.port.out.ProjectDiagramRepository;
import com.libreuml.backend.domain.model.ProjectDiagram;
import com.libreuml.backend.infrastructure.out.persistence.entity.ProjectDiagramEntity;
import com.libreuml.backend.infrastructure.out.persistence.repository.SpringDataProjectDiagramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProjectDiagramPersistenceAdapter implements ProjectDiagramRepository {

    private final SpringDataProjectDiagramRepository jpaRepository;

    @Override
    public ProjectDiagram save(ProjectDiagram diagram) {
        ProjectDiagramEntity saved = jpaRepository.saveAndFlush(toEntity(diagram));
        return toDomain(saved);
    }

    @Override
    public Optional<ProjectDiagram> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ProjectDiagram> findAllByProjectId(UUID projectId) {
        return jpaRepository.findAllByProjectIdOrderByUpdatedAtDesc(projectId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private ProjectDiagramEntity toEntity(ProjectDiagram diagram) {
        return ProjectDiagramEntity.builder()
                .id(diagram.getId())
                .projectId(diagram.getProjectId())
                .name(diagram.getName())
                .diagramType(diagram.getDiagramType())
                .path(diagram.getPath())
                .viewData(diagram.getViewData())
                .version(diagram.getVersion())
                .createdAt(diagram.getCreatedAt())
                .updatedAt(diagram.getUpdatedAt())
                .build();
    }

    private ProjectDiagram toDomain(ProjectDiagramEntity entity) {
        return ProjectDiagram.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .name(entity.getName())
                .diagramType(entity.getDiagramType())
                .path(entity.getPath())
                .viewData(entity.getViewData())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
