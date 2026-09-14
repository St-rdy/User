package com.stardy.user.global;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    // AccessToken 생성
    public String createAccessToken(String socialId, String provider, String role){
        return Jwts.builder()
                .subject(socialId)
                .claim("provider", provider)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(secretKey)
                .compact();
    }

    // RefreshToken 생성
    public String createRefreshToken(String socialId, String provider) {
        return Jwts.builder()
                .subject(socialId)
                .claim("provider", provider)
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(secretKey)
                .compact();
    }

    // 토큰에서 이메일 추출
    public String extractSocialId(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractProvider(String token) {
        return parseClaims(token).get("provider", String.class);
    }

    // 기존 호출부의 컴파일 호환용 별칭입니다. 이메일이 아니라 JWT subject를 반환합니다.
    @Deprecated
    public String extractEmail(String token) {
        return extractSocialId(token);
    }

    @Deprecated
    public String createAccessToken(String socialId, String role) {
        return createAccessToken(socialId, "LEGACY", role);
    }

    @Deprecated
    public String createRefreshToken(String socialId) {
        return createRefreshToken(socialId, "LEGACY");
    }

    // 토큰 유효성 검사
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 토큰에서 클레임 추출
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 토큰에서 역할 추출
    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }
}
