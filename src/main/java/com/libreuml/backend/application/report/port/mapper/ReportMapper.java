package com.libreuml.backend.application.report.port.mapper;

import com.libreuml.backend.application.report.port.in.dto.CreateReportCommand;
import com.libreuml.backend.application.report.port.in.dto.ResponseAndUpdateStatusCommand;
import com.libreuml.backend.application.report.port.in.dto.UpdateReportPriorityCommand;
import com.libreuml.backend.application.report.port.in.dto.UpdateReportStatusCommand;
import com.libreuml.backend.domain.model.Report;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReportMapper {

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "status", constant = "OPEN")
    @Mapping(target = "priority", constant = "NONE")
    Report toDomain(CreateReportCommand createReportCommand);

    @Mapping(target = "id", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void respondReport(ResponseAndUpdateStatusCommand command, @MappingTarget Report report);

    @Mapping(target = "id", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateReportStatus(UpdateReportStatusCommand command, @MappingTarget Report report);

    @Mapping(target = "id", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateReportPriority(UpdateReportPriorityCommand command, @MappingTarget Report report);

}
