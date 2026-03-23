package com.libreuml.backend.infrastructure.out.persistence.adapter;

import com.libreuml.backend.application.diagram.port.out.DiagramRepository;
import com.libreuml.backend.domain.model.Diagram;
import com.libreuml.backend.infrastructure.out.persistence.entity.DiagramEntity;
import com.libreuml.backend.infrastructure.out.persistence.repository.SpringDataDiagramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter for {@link Diagram} aggregates.
 *
 * <p>Mapping is done manually (no MapStruct) because the {@code content} field is a
 * Jackson {@link com.fasterxml.jackson.databind.node.ObjectNode}, which MapStruct cannot
 * map without a custom converter anyway.  Manual mapping keeps the translation explicit and
 * easy to reason about.
 *
 * <p>For <em>new</em> diagrams, the {@link Diagram} arrives with {@code id == null}.
 * {@link DiagramEntity} carries {@code @GeneratedValue(strategy = GenerationType.UUID)},
 * so Spring Data JPA's {@code isNew()} check (ID is null → call {@code persist()}) causes
 * Hibernate to generate the UUID and execute an INSERT.  On subsequent saves (updates), the
 * non-null ID triggers {@code merge()}, which generates an optimistic-lock
 * {@code UPDATE … WHERE id = ? AND version = ?}.
 */
@Component
@RequiredArgsConstructor
public class DiagramPersistenceAdapter implements DiagramRepository {

    private final SpringDataDiagramRepository jpaRepository;

    @Override
    public Diagram save(Diagram diagram) {
        DiagramEntity entity = toEntity(diagram);
        // saveAndFlush forces an immediate flush so Hibernate increments the @Version counter
        // before this method returns.  Without it, the flush happens at transaction commit
        // (after the service method has already built the response DTO), which would cause
        // the returned Diagram to carry the pre-update version number.
        DiagramEntity saved = jpaRepository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Diagram> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Diagram> findByOwnerId(UUID ownerId) {
        return jpaRepository.findByOwnerId(ownerId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private DiagramEntity toEntity(Diagram diagram) {
        return DiagramEntity.builder()
                .id(diagram.getId())
                .ownerId(diagram.getOwnerId())
                .title(diagram.getTitle())
                .type(diagram.getType())
                .visibility(diagram.getVisibility())
                .content(diagram.getContent())
                .version(diagram.getVersion())
                .createdAt(diagram.getCreatedAt())
                .updatedAt(diagram.getUpdatedAt())
                .collaboratorIds(new HashSet<>(diagram.getCollaboratorIds()))
                .build();
    }

    private Diagram toDomain(DiagramEntity entity) {
        return Diagram.builder()
                .id(entity.getId())
                .ownerId(entity.getOwnerId())
                .title(entity.getTitle())
                .type(entity.getType())
                .visibility(entity.getVisibility())
                .content(entity.getContent())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .collaboratorIds(new HashSet<>(entity.getCollaboratorIds()))
                .build();
    }
}
