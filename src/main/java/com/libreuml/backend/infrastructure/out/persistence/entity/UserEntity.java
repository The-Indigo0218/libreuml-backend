package com.libreuml.backend.infrastructure.out.persistence.entity;

import com.libreuml.backend.domain.model.RoleEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(builderMethodName = "userEntityBuilder")
@EntityListeners(AuditingEntityListener.class)
public class UserEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name")
    private String fullName;

    @Enumerated(EnumType.STRING)
    private RoleEnum role;

    private Boolean active;

    @Column(name = "joined_at")
    private LocalDate joinedAt;

    @Column(name = "last_login")
    private LocalDate lastLogin;

    @Column(name = "avatar_url")
    private String avatarUrl;


    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "social_profile_id", referencedColumnName = "id")
    private SocialProfileEntity socialProfile;


    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_academic_degrees", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "degree")
    private List<String> academicDegrees;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_organizations", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "organization")
    private List<String> organization;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_stacks", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "stack_technology")
    private List<String> stacks;
}