package com.stardy.user.dto;

import java.util.Map;

public record OAuthSignupRequestDto(
        String nickname,
        String profileImageUrl,
        Map<String, Object> domain
) {
}
