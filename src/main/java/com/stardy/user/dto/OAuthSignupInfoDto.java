package com.stardy.user.dto;

public record OAuthSignupInfoDto(
        String email,
        String name,
        String role,
        String provider,
        String socialId,
        String providerEmail
) {
}
