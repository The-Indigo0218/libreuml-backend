package com.libreuml.backend.application.diagram.port.service;

import com.libreuml.backend.application.common.port.out.MetricsPort;
import com.libreuml.backend.application.diagram.dto.CreateDiagramCommand;
import com.libreuml.backend.application.diagram.dto.UpdateDiagramCommand;
import com.libreuml.backend.application.diagram.exception.DiagramConflictException;
import com.libreuml.backend.application.diagram.exception.DiagramNotFoundException;
import com.libreuml.backend.application.diagram.port.in.CreateDiagramUseCase;
import com.libreuml.backend.application.diagram.port.in.DeleteDiagramUseCase;
import com.libreuml.backend.application.diagram.port.in.GetDiagramUseCase;
import com.libreuml.backend.application.diagram.port.in.UpdateDiagramUseCase;
import com.libreuml.backend.application.diagram.port.out.DiagramRepository;
import com.libreuml.backend.domain.model.Diagram;
import com.libreuml.backend.domain.model.exception.DiagramOwnershipException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DiagramService implements CreateDiagramUseCase, GetDiagramUseCase,
        UpdateDiagramUseCase, DeleteDiagramUseCase {

    private final DiagramRepository diagramRepository;
    private final MetricsPort metricsPort;

    @Override
    public Diagram create(CreateDiagramCommand command) {
        Diagram diagram = Diagram.create(
                command.ownerId(), command.title(), command.type(), command.content());
        Diagram saved = diagramRepository.save(diagram);
        metricsPort.incrementDiagramSaved(saved.getType());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Diagram findById(UUID diagramId, UUID requesterId) {
        Diagram diagram = diagramRepository.findById(diagramId)
                .orElseThrow(() -> new DiagramNotFoundException("Diagram not found: " + diagramId));
        if (!diagram.isAccessibleBy(requesterId)) {
            throw new DiagramOwnershipException("Access denied to diagram: " + diagramId);
        }
        return diagram;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Diagram> listByOwner(UUID ownerId) {
        return diagramRepository.findByOwnerId(ownerId);
    }

    @Override
    public Diagram update(UpdateDiagramCommand command) {
        Diagram diagram = diagramRepository.findById(command.diagramId())
                .orElseThrow(() -> new DiagramNotFoundException("Diagram not found: " + command.diagramId()));

        /*
         * Application-level optimistic concurrency guard: if the client's version does not
         * match the version read from the database, a concurrent edit has occurred.  We fail
         * fast here rather than relying solely on the JPA @Version UPDATE check, making the
         * 409 response deterministic and testable without true concurrency in the test suite.
         * JPA's @Version mechanism still acts as a final safety net for genuine concurrent
         * requests that slip past this check within the same transaction window.
         */
        if (diagram.getVersion() != command.version()) {
            throw new DiagramConflictException(
                    "Version mismatch: client sent " + command.version()
                    + " but current version is " + diagram.getVersion()
                    + ". Reload the diagram and retry.");
        }

        diagram.update(command.title(), command.content(), command.requesterId());
        return diagramRepository.save(diagram);
    }

    @Override
    public void delete(UUID diagramId, UUID requesterId) {
        Diagram diagram = diagramRepository.findById(diagramId)
                .orElseThrow(() -> new DiagramNotFoundException("Diagram not found: " + diagramId));
        diagram.delete(requesterId);
        diagramRepository.deleteById(diagramId);
    }
}
