package com.libreuml.backend.infrastructure.out.persistence.adapter;

import com.libreuml.backend.application.project.port.out.SemanticModelRepository;
import com.libreuml.backend.domain.model.SemanticModel;
import com.libreuml.backend.infrastructure.out.persistence.entity.SemanticModelEntity;
import com.libreuml.backend.infrastructure.out.persistence.repository.SpringDataSemanticModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SemanticModelPersistenceAdapter implements SemanticModelRepository {

    private final SpringDataSemanticModelRepository jpaRepository;

    @Override
    public SemanticModel save(SemanticModel model) {
        SemanticModelEntity saved = jpaRepository.saveAndFlush(toEntity(model));
        return toDomain(saved);
    }

    @Override
    public Optional<SemanticModel> findByProjectId(UUID projectId) {
        return jpaRepository.findByProjectId(projectId).map(this::toDomain);
    }

    private SemanticModelEntity toEntity(SemanticModel model) {
        return SemanticModelEntity.builder()
                .id(model.getId())
                .projectId(model.getProjectId())
                .data(model.getData())
                .version(model.getVersion())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }

    private SemanticModel toDomain(SemanticModelEntity entity) {
        return SemanticModel.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .data(entity.getData())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
