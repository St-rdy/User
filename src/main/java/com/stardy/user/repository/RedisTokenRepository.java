package com.stardy.user.repository;

import com.stardy.user.dto.TokenResponseDto;

public interface RedisTokenRepository {
    // JWT 코드 관련 Redis 작업
    void saveRefreshToken(String email, String refreshToken);
    String getRefreshToken(String email);
    void deleteRefreshToken(String email);

    // 임시 코드 관련 Redis 작업
    void saveTemporaryCode(String tempCode, TokenResponseDto tokens);
    TokenResponseDto getTemporaryCode(String tempCode);
    void deleteTemporaryCode(String tempCode);
}
