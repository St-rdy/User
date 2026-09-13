package com.stardy.user.dto;

public record AccessTokenResponseDto(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
