package com.stardy.user.dto;

/**
 * @author Jinwook Jung
 * @version 1.0
 * @since 2026-02-23
 */
public class TokenResponse {
    private final String accessToken;
    private final String refreshToken;

    public TokenResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
