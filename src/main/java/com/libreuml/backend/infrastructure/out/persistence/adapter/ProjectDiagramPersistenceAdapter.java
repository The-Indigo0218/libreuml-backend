package com.libreuml.backend.infrastructure.out.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.application.projectdiagram.port.out.ProjectDiagramRepository;
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
    private final ObjectMapper objectMapper;

    @Override
    public ProjectDiagram save(ProjectDiagram diagram) {
        ProjectDiagramEntity entity = toEntity(diagram);
        return toDomain(jpaRepository.saveAndFlush(entity));
    }

    @Override
    public Optional<ProjectDiagram> findByProjectIdAndId(UUID projectId, UUID diagramId) {
        return jpaRepository.findByProjectIdAndId(projectId, diagramId).map(this::toDomain);
    }

    @Override
    public List<ProjectDiagram> findAllByProjectId(UUID projectId) {
        return jpaRepository.findAllByProjectId(projectId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID diagramId) {
        jpaRepository.deleteById(diagramId);
    }

    @Override
    public long getTotalViewDataBytesByOwner(UUID ownerId) {
        return jpaRepository.getTotalViewDataBytesByOwner(ownerId);
    }

    private ProjectDiagramEntity toEntity(ProjectDiagram d) {
        ObjectNode viewData = d.getViewData() != null
                ? d.getViewData()
                : objectMapper.createObjectNode();
        return ProjectDiagramEntity.builder()
                .id(d.getId())
                .projectId(d.getProjectId())
                .name(d.getName())
                .diagramType(d.getDiagramType())
                .path(d.getPath())
                .viewData(viewData)
                .version(d.getVersion())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    private ProjectDiagram toDomain(ProjectDiagramEntity e) {
        return ProjectDiagram.builder()
                .id(e.getId())
                .projectId(e.getProjectId())
                .name(e.getName())
                .diagramType(e.getDiagramType())
                .path(e.getPath())
                .viewData(e.getViewData())
                .version(e.getVersion())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
