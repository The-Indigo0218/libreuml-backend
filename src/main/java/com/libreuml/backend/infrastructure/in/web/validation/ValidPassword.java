package com.libreuml.backend.infrastructure.in.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@NotBlank(message = "Password is required")
@Size(min = 8, message = "Password must be at least 8 characters long")
@Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$",
        message = "Password must contain at least one digit, one lowercase letter, one uppercase letter, and one special character"
)
public @interface ValidPassword {
    String message() default "Password does not meet complexity requirements";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
