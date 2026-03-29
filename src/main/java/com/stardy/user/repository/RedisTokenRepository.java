package com.stardy.user.repository;

public interface RedisTokenRepository {
    void saveRefreshToken(String email, String refreshToken);
    String getRefreshToken(String email);
    void deleteRefreshToken(String email);
}
