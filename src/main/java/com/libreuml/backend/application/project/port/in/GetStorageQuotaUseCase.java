package com.libreuml.backend.application.project.port.in;

import java.util.UUID;

public interface GetStorageQuotaUseCase {
    record QuotaInfo(long quota, long used, long available, long modelsBytes, long diagramsBytes) {}
    QuotaInfo getQuota(UUID userId);
}
