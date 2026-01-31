package com.libreuml.backend.infrastructure.out.persistence.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;

@Entity
@DiscriminatorValue("DEVELOPER")
@Getter
@Setter
@NoArgsConstructor
public class DeveloperEntity extends UserEntity {
}