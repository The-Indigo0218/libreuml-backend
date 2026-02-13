package com.libreuml.backend.application.user.port.mapper;


import com.libreuml.backend.application.user.port.in.dto.*;
import com.libreuml.backend.domain.model.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateBasicInfoFromCommand(UpdateUserBasicInfoCommand command, @MappingTarget User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "socialProfile.githubUrl", source = "githubUrl")
    @Mapping(target = "socialProfile.instagramUrl", source = "instagramUrl")
    @Mapping(target = "socialProfile.XUrl", source = "xUrl")
    @Mapping(target = "socialProfile.linkedinUrl", source = "linkedinUrl")
    @Mapping(target = "socialProfile.webSiteUrl", source = "webSiteUrl")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateSocialProfileFromCommand(UpdateSocialProfileCommand command, @MappingTarget User user);

    @Mapping(target = "id", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEmailFromCommand(UpdateEmailCommand command, @MappingTarget User user);

    @Mapping(target = "id", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProfilePictureFromCommand(UpdateProfilePictureCommand command, @MappingTarget User user);

}
