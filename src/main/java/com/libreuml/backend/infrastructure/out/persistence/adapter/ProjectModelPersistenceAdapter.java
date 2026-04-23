package com.libreuml.backend.infrastructure.out.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.application.projectmodel.port.out.ProjectModelRepository;
import com.libreuml.backend.domain.model.ProjectModel;
import com.libreuml.backend.infrastructure.out.persistence.entity.ProjectModelEntity;
import com.libreuml.backend.infrastructure.out.persistence.repository.SpringDataProjectModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProjectModelPersistenceAdapter implements ProjectModelRepository {

    private final SpringDataProjectModelRepository jpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public ProjectModel save(ProjectModel model) {
        ProjectModelEntity entity = toEntity(model);
        return toDomain(jpaRepository.saveAndFlush(entity));
    }

    @Override
    public Optional<ProjectModel> findByProjectId(UUID projectId) {
        return jpaRepository.findByProjectId(projectId).map(this::toDomain);
    }

    @Override
    public long getModelDataBytesByProjectId(UUID projectId) {
        return jpaRepository.getModelDataBytesByProjectId(projectId);
    }

    @Override
    public long getTotalModelDataBytesByOwner(UUID userId) {
        return jpaRepository.getTotalModelDataBytesByOwner(userId);
    }

    private ProjectModelEntity toEntity(ProjectModel m) {
        ObjectNode data = m.getModelData() != null
                ? m.getModelData()
                : objectMapper.createObjectNode();
        return ProjectModelEntity.builder()
                .id(m.getId())
                .projectId(m.getProjectId())
                .modelData(data)
                .version(m.getVersion())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    private ProjectModel toDomain(ProjectModelEntity e) {
        return ProjectModel.builder()
                .id(e.getId())
                .projectId(e.getProjectId())
                .modelData(e.getModelData())
                .version(e.getVersion())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
