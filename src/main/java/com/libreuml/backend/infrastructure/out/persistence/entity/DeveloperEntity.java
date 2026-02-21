package com.libreuml.backend.infrastructure.out.persistence.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("DEVELOPER")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class DeveloperEntity extends UserEntity {
}
