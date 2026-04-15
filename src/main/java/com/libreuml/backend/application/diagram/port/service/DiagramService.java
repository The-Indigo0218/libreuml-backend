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
import com.libreuml.backend.application.user.exception.UserNotFoundException;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.Diagram;
import com.libreuml.backend.domain.model.User;
import com.libreuml.backend.domain.model.exception.DiagramOwnershipException;
import com.libreuml.backend.domain.model.exception.QuotaExceededException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DiagramService implements CreateDiagramUseCase, GetDiagramUseCase,
        UpdateDiagramUseCase, DeleteDiagramUseCase {

    private final DiagramRepository diagramRepository;
    private final UserRepository userRepository;
    private final MetricsPort metricsPort;

    @Override
    public Diagram create(CreateDiagramCommand command) {
        User user = userRepository.getUserById(command.ownerId())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + command.ownerId()));

        // Compute UTF-8 payload size using the same mechanism as Diagram.assertPayloadSize().
        long payloadBytes = command.content() != null
                ? command.content().toString().getBytes(StandardCharsets.UTF_8).length
                : 0L;

        if (!user.hasQuotaFor(payloadBytes)) {
            throw new QuotaExceededException(
                    "Storage quota exceeded. Quota: " + user.getStorageQuotaBytes()
                    + " bytes, already used: " + user.getStorageUsedBytes()
                    + " bytes, requested: " + payloadBytes + " bytes.");
        }

        Diagram diagram = Diagram.create(
                command.ownerId(), command.title(), command.type(), command.content());
        Diagram saved = diagramRepository.save(diagram);

        user.incrementUsage(payloadBytes);
        userRepository.save(user);

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

        long payloadBytes = diagram.getContent() != null
                ? diagram.getContent().toString().getBytes(StandardCharsets.UTF_8).length
                : 0L;

        diagramRepository.deleteById(diagramId);

        User user = userRepository.getUserById(requesterId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + requesterId));
        user.decrementUsage(payloadBytes);
        userRepository.save(user);
    }
}
