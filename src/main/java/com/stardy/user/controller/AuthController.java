package com.stardy.user.controller;

import com.stardy.user.dto.AccessTokenResponseDto;
import com.stardy.user.dto.MessageResponseDto;
import com.stardy.user.dto.OAuthSignupRequestDto;
import com.stardy.user.dto.TokenResponseDto;
import com.stardy.user.exception.BaseException;
import com.stardy.user.global.JwtProvider;
import com.stardy.user.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

import static com.stardy.user.exception.ErrorCode.REFRESH_TOKEN_NOT_FOUND;
import static com.stardy.user.exception.ErrorCode.UNAUTHORIZED;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;
    private final JwtProvider jwtProvider;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public AuthController(
            AuthService authService,
            JwtProvider jwtProvider,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {
        this.authService = authService;
        this.jwtProvider = jwtProvider;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    @GetMapping("/token")
    public ResponseEntity<AccessTokenResponseDto> exchangeTemporaryCode(@RequestParam String code) {
        TokenResponseDto tokens = authService.exchangeTemporaryCode(code);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(tokens.getRefreshToken()).toString())
                .body(createAccessTokenResponse(tokens.getAccessToken()));
    }

    @PostMapping("/signup")
    public ResponseEntity<MessageResponseDto> completeSignup(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody OAuthSignupRequestDto request
    ) {
        String email = jwtProvider.extractEmail(extractBearerToken(authorization));
        authService.completeSignup(email, request.nickname(), request.profileImageUrl(), request.domain());
        return ResponseEntity.ok(new MessageResponseDto("가입이 완료되었습니다."));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<AccessTokenResponseDto> reissueToken(
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BaseException(REFRESH_TOKEN_NOT_FOUND);
        }

        TokenResponseDto tokens = authService.reissueToken(refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(tokens.getRefreshToken()).toString())
                .body(createAccessTokenResponse(tokens.getAccessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponseDto> logout(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        String accessToken = extractBearerToken(authorization);
        String email = jwtProvider.extractEmail(accessToken);

        authService.logout(email);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expireRefreshTokenCookie().toString())
                .body(new MessageResponseDto("로그아웃이 완료되었습니다."));
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BaseException(UNAUTHORIZED);
        }

        String token = authorization.substring(BEARER_PREFIX.length());
        if (token.isBlank() || !jwtProvider.isTokenValid(token)) {
            throw new BaseException(UNAUTHORIZED);
        }

        return token;
    }

    private AccessTokenResponseDto createAccessTokenResponse(String accessToken) {
        return new AccessTokenResponseDto(accessToken, "Bearer", accessTokenExpiration / 1000);
    }

    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(AUTH_COOKIE_PATH)
                .maxAge(Duration.ofMillis(refreshTokenExpiration))
                .build();
    }

    private ResponseCookie expireRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(AUTH_COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }
}
