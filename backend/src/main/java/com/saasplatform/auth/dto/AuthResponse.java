package com.saasplatform.auth.dto;

import java.util.UUID;

/**
 * Auth response containing JWT access token and refresh token.
 * The refresh token should be stored as HttpOnly cookie in production.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,         // seconds
        UserDto user,
        BusinessDto business
) {
    public record UserDto(
            UUID id,
            String email,
            String firstName,
            String lastName,
            String role
    ) {}

    public record BusinessDto(
            UUID id,
            String name,
            String slug,
            String businessType
    ) {}
}
