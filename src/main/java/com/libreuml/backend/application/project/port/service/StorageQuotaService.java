package com.libreuml.backend.application.project.port.service;

import com.libreuml.backend.application.project.port.in.GetStorageQuotaUseCase;
import com.libreuml.backend.application.projectdiagram.port.out.ProjectDiagramRepository;
import com.libreuml.backend.application.projectmodel.port.out.ProjectModelRepository;
import com.libreuml.backend.application.user.exception.UserNotFoundException;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StorageQuotaService implements GetStorageQuotaUseCase {

    private final UserRepository userRepository;
    private final ProjectModelRepository projectModelRepository;
    private final ProjectDiagramRepository projectDiagramRepository;

    @Override
    public QuotaInfo getQuota(UUID userId) {
        User user = userRepository.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        long quota = user.getStorageQuotaBytes();
        long legacyDiagramBytes = user.getStorageUsedBytes();
        long modelsBytes = projectModelRepository.getTotalModelDataBytesByOwner(userId);
        long diagramsBytes = projectDiagramRepository.getTotalViewDataBytesByOwner(userId);
        long used = legacyDiagramBytes + modelsBytes + diagramsBytes;

        return new QuotaInfo(quota, used, quota - used, modelsBytes, diagramsBytes);
    }
}
