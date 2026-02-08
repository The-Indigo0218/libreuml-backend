package com.libreuml.backend.infrastructure.out.persistence.adapter;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.application.resource.port.out.ResourceRepository;
import com.libreuml.backend.domain.model.Resource;
import com.libreuml.backend.infrastructure.out.persistence.entity.ResourceEntity;
import com.libreuml.backend.infrastructure.out.persistence.mapper.ResourcePersistenceMapper;
import com.libreuml.backend.infrastructure.out.persistence.repository.SpringDataResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ResourcePersistenceAdapter implements ResourceRepository {

    private final SpringDataResourceRepository resourceRepository;
    private final ResourcePersistenceMapper resourceMapper;

    @Override
    public Resource save(Resource resource) {
        ResourceEntity entity = resourceMapper.toEntity(resource);
        return resourceMapper.toDomain(resourceRepository.save(entity));
    }

    @Override
    public Optional<Resource> findById(UUID id) {
        return resourceRepository.findById(id)
                .map(resourceMapper::toDomain);
    }

    @Override
    public PagedResult<Resource> findAllByCreatorId(UUID creatorId, PaginationCommand pagination) {
        return null;
    }
}