package com.libreuml.backend.application.resource.port.service;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.exception.UserNotAuthorizedException;
import com.libreuml.backend.application.resource.exception.ResourceNotFoundException;
import com.libreuml.backend.application.resource.port.in.CreateResourceUseCase;
import com.libreuml.backend.application.resource.port.in.GetResourceUseCase;
import com.libreuml.backend.application.resource.port.in.UpdateResourceUseCase;
import com.libreuml.backend.application.resource.port.in.dto.*;
import com.libreuml.backend.application.resource.port.mapper.ResourceMapper;
import com.libreuml.backend.application.resource.port.out.ResourceRepository;
import com.libreuml.backend.application.user.exception.UserNotFoundException;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.Resource;
import com.libreuml.backend.domain.model.User;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
public class ResourceService implements CreateResourceUseCase, UpdateResourceUseCase, GetResourceUseCase {

    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final ResourceMapper resourceMapper;


    @Override
    public Resource create(CreateResourceCommand command) {
        getUserOrThrow(command.creatorId());
        Resource resource = resourceMapper.toResource(command);
        return resourceRepository.save(resource);
    }

    @Override
    public Resource getResourceById(GetResourceByIdCommand command) {
        return getResourceOrThrow(command.resourceId());
    }

    @Override
    public PagedResult<Resource> getResourceByTitle(GetResourceByTitleCommand command) {
        return resourceRepository.findAllByTitleContaining(command.title(), command.pagination());
    }

    @Override
    public PagedResult<Resource> getResourceByTag(GetResourceByTagCommand command) {
        return resourceRepository.findAllByTagsContaining(command.tag(), command.pagination());
    }

    @Override
    public PagedResult<Resource> getResourceByCreator(GetResourceByCreatorIdCommand command) {
        return resourceRepository.findAllByCreatorId(command.creatorId(), command.pagination());
    }

    @Override
    public Long countResourceByTag(String tag) {
        return resourceRepository.countByTagsContaining(tag);
    }

    @Override
    public Long totalActiveResources() {
        return resourceRepository.totalActiveResources();
    }

    @Override
    public Resource updateTitleAndContent(UpdateTitleAndContentResourceCommand command) {
        Resource resource = getResourceOrThrow(command.resourceId());
        verifyUserIsCreator(getUserOrThrow(command.userId()), resource);
        resourceMapper.updateTitleAndContentFromCommand(command, resource);
        return resourceRepository.save(resource);
    }

    @Override
    public Resource deactivateResource(DeactivateResourceCommand command) {
        Resource resource = getResourceOrThrow(command.resourceId());
        verifyUserIsCreator(getUserOrThrow(command.userId()), resource);
        resource.deactivate();
        return resourceRepository.save(resource);
    }

    @Override
    public Resource updateResourceTags(UpdateTagsResourceCommand command) {
        Resource resource = getResourceOrThrow(command.resourceId());
        verifyUserIsCreator(getUserOrThrow(command.userId()), resource);
        resourceMapper.updateTagsFromCommand(command, resource);
        return resourceRepository.save(resource);
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException("User with id " + userId + " not found"));
    }

    private Resource getResourceOrThrow(UUID resourceId) {
        return resourceRepository.findById(resourceId).orElseThrow(() -> new ResourceNotFoundException("Resource with id " + resourceId + " not found"));
    }

    private void verifyUserIsCreator(User user, Resource resource) {
        if (!resource.getCreatorId().equals(user.getId())) {
            throw new UserNotAuthorizedException(" User with id " + user.getId() + " is not authorized to modify resource with id " + resource.getId());
        }
    }
}
