package com.libreuml.backend.infrastructure.out.persistence.mapper;

import com.libreuml.backend.domain.model.*;
import com.libreuml.backend.infrastructure.out.persistence.entity.*;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserEntity toEntity(User userDomain) {
        if (userDomain instanceof Student) {
            StudentEntity entity = new StudentEntity();
            mapBaseFields(userDomain, entity);
            return entity;
        } else if (userDomain instanceof Teacher teacherDomain) {
            TeacherEntity entity = new TeacherEntity();
            mapBaseFields(userDomain, entity);
            entity.setCode(teacherDomain.getCode());
            entity.setStudentCount(teacherDomain.getStudentCount());
            return entity;
        } else if (userDomain instanceof Developer) {
            DeveloperEntity entity = new DeveloperEntity();
            mapBaseFields(userDomain, entity);
            return entity;
        }
        throw new IllegalArgumentException("Unknown domain user type");
    }

    public User toDomain(UserEntity entity) {
        if (entity instanceof StudentEntity) {
            return Student.builder()
                    .id(entity.getId())
                    .email(entity.getEmail())
                    .password(entity.getPassword())
                    .fullName(entity.getFullName())
                    .role(entity.getRole())
                    .active(entity.getActive())
                    .joinedAt(entity.getJoinedAt())
                    .build();
        } else if (entity instanceof TeacherEntity teacherEntity) {
            return Teacher.builder()
                    .id(entity.getId())
                    .email(entity.getEmail())
                    .password(entity.getPassword())
                    .fullName(entity.getFullName())
                    .role(entity.getRole())
                    .active(entity.getActive())
                    .joinedAt(entity.getJoinedAt())
                    .code(teacherEntity.getCode())
                    .studentCount(teacherEntity.getStudentCount())
                    .build();
        } else if (entity instanceof DeveloperEntity) {
            return Developer.builder()
                    .id(entity.getId())
                    .email(entity.getEmail())
                    .password(entity.getPassword())
                    .fullName(entity.getFullName())
                    .role(entity.getRole())
                    .active(entity.getActive())
                    .joinedAt(entity.getJoinedAt())
                    .build();
        }
        throw new IllegalArgumentException("Unknown entity type");
    }

    private void mapBaseFields(User source, UserEntity target) {
        target.setId(source.getId());
        target.setEmail(source.getEmail());
        target.setPassword(source.getPassword());
        target.setFullName(source.getFullName());
        target.setRole(source.getRole());
        target.setActive(source.getActive());
        target.setJoinedAt(source.getJoinedAt());
        target.setAcademicDegrees(source.getAcademicDegrees());
        target.setOrganization(source.getOrganization());
        target.setStacks(source.getStacks());
        if (source.getSocialProfile() != null) {
            target.setSocialProfile(toSocialEntity(source.getSocialProfile()));
        }
    }

    private SocialProfileEntity toSocialEntity(SocialProfile domain) {
        if (domain == null) return null;
        return SocialProfileEntity.builder()
                .githubUrl(domain.getGithubUrl())
                .instagramUrl(domain.getInstagramUrl())
                .linkedinUrl(domain.getLinkedinUrl())
                .xUrl(domain.getXUrl())
                .websiteUrl(domain.getWebSiteUrl())
                .build();
    }

    private SocialProfile toSocialDomain(SocialProfileEntity entity) {
        if (entity == null) return null;
        return SocialProfile.builder()
                .githubUrl(entity.getGithubUrl())
                .instagramUrl(entity.getInstagramUrl())
                .linkedinUrl(entity.getLinkedinUrl())
                .xUrl(entity.getXUrl())
                .webSiteUrl(entity.getWebsiteUrl())
                .build();
    }

}