package com.libreuml.backend.application.resource.port.mapper;

import com.libreuml.backend.application.resource.port.in.dto.CreateResourceCommand;
import com.libreuml.backend.application.resource.port.in.dto.UpdateTagsResourceCommand;
import com.libreuml.backend.application.resource.port.in.dto.UpdateTitleAndContentResourceCommand;
import com.libreuml.backend.domain.model.Resource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface ResourceMapper {

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "creatorId", source = "creatorId")
    Resource toResource(CreateResourceCommand command);


    void updateTagsFromCommand(UpdateTagsResourceCommand command, @MappingTarget Resource resource);

    void updateTitleAndContentFromCommand(UpdateTitleAndContentResourceCommand command, @MappingTarget Resource resource);

}
