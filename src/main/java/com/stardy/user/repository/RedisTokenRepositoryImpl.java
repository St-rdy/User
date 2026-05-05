package com.stardy.user.repository;

import com.stardy.user.dto.TokenResponseDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
public class RedisTokenRepositoryImpl implements RedisTokenRepository {
    private final RedisTemplate<String, String> redisTemplate;
    private final long refreshTokenExpiration;
    private final long temporaryCodeExpiration;

    public RedisTokenRepositoryImpl(
            RedisTemplate<String, String> redisTemplate,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration,
            @Value("${jwt.temporary-code-expiration}") long temporaryCodeExpiration
    ) {
        this.redisTemplate = redisTemplate;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.temporaryCodeExpiration = temporaryCodeExpiration;
    }

    public void saveRefreshToken(String email, String refreshToken) {
        redisTemplate.opsForValue().set(email, refreshToken, refreshTokenExpiration, TimeUnit.MILLISECONDS);
    }

    public String getRefreshToken(String email) {
        return redisTemplate.opsForValue().get(email);
    }

    public void deleteRefreshToken(String email) {
        redisTemplate.delete(email);
    }

    @Override
    public void saveTemporaryCode(String tempCode, TokenResponseDto tokens) {
        // TokenResponse를 "accessToken|refreshToken" 형태로 직렬화해서 저장
        // Redis는 문자열만 저장하기 때문에 두 값을 구분자로 이어붙임
        String value = tokens.getAccessToken() + "|" + tokens.getRefreshToken();
        redisTemplate.opsForValue().set(
                // 키 충돌 방지를 위해 "temp:" 접두사를 붙임
                // RefreshToken 키(email)와 임시 코드 키(UUID)가 충돌하지 않도록 네임스페이스 분리
                "temp:" + tempCode,
                value,
                temporaryCodeExpiration,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public TokenResponseDto getTemporaryCode(String tempCode) {
        String value = redisTemplate.opsForValue().get("temp:" + tempCode);

        // Redis에 없으면 null 반환 — AuthService에서 null 체크 후 예외 처리
        if (value == null) {
            return null;
        }

        // "accessToken|refreshToken" 형태로 저장했으니 다시 split해서 복원
        String[] parts = value.split("\\|");
        return new TokenResponseDto(parts[0], parts[1]);
    }

    @Override
    public void deleteTemporaryCode(String tempCode) {
        redisTemplate.delete("temp:" + tempCode);
    }
}
