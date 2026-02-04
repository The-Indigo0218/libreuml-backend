package com.libreuml.backend.application.question.port.mapper;

import com.libreuml.backend.application.question.port.in.dto.CreateQuestionCommand;
import com.libreuml.backend.application.question.port.in.dto.UpdateSolvedStatusCommand;
import com.libreuml.backend.application.question.port.in.dto.UpdateTitleAndContentCommand;
import com.libreuml.backend.domain.model.Question;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface QuestionMapper {

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "creatorId", source = "creatorId")
    @Mapping(target = "imageUrls", source = "imageUrls", defaultValue = "java.util.Collections.emptyList()")
    Question toDomain(CreateQuestionCommand command);

    @Mapping(target = "id", ignore = true)
    void updateFromCommand(UpdateTitleAndContentCommand command, @MappingTarget Question question);

    @Mapping(target = "id", ignore = true)
    void updateFromCommand(UpdateSolvedStatusCommand command, @MappingTarget Question question);
}