package com.saasplatform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for user registration.
 * Creates both a User and a Business (first tenant registration flow).
 * businessType is intentionally omitted — always defaults to OTHER.
 */
public record RegisterRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name too long")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name too long")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit"
        )
        String password,

        @NotBlank(message = "Business name is required")
        @Size(max = 255, message = "Business name too long")
        String businessName
) {}
