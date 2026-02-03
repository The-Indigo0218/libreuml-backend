package com.libreuml.backend.application.answer.port.mapper;

import com.libreuml.backend.application.answer.port.in.dto.CreateAnswerCommand;
import com.libreuml.backend.application.answer.port.in.dto.UpdateAnswerContentCommand;
import com.libreuml.backend.domain.model.Answer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AnswerMapper {

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "creatorId", source = "creatorId")
    @Mapping(target = "imageUrls", source = "imageUrls", defaultValue = "java.util.Collections.emptyList()")
    @Mapping(target = "questionId", source = "questionId")
    Answer toDomain(CreateAnswerCommand command);

    void updateContentFromCommand(UpdateAnswerContentCommand command, @MappingTarget Answer answer);
}
