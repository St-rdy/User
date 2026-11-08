package com.stardy.user.global;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/*
생성  →  createAccessToken(), createRefreshToken()
검증  →  isTokenValid()
추출  →  extractSocialId(), extractProvider(), extractRole()

@author Jinwook Jung
 */
class JwtProviderTest {
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("발급한 JWT에는 서명된 토큰 용도가 포함된다.")
    void issuedTokensContainTokenType() {
        var parser = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor("5f7e12f7-a7fa-4cf3-8831-7656034e90d3".getBytes(StandardCharsets.UTF_8)))
                .build();

        String accessToken = jwtProvider.createAccessToken("google-sub", "GOOGLE", "ROLE_USER");
        String refreshToken = jwtProvider.createRefreshToken("google-sub", "GOOGLE");

        assertThat(parser.parseSignedClaims(accessToken).getPayload().get("tokenType", String.class)).isEqualTo("ACCESS");
        assertThat(parser.parseSignedClaims(refreshToken).getPayload().get("tokenType", String.class)).isEqualTo("REFRESH");
    }

    @Test
    @DisplayName("Access Token과 Refresh Token은 각자의 용도로만 검증된다.")
    void validateTokensOnlyForTheirPurpose() {
        String accessToken = jwtProvider.createAccessToken("google-sub", "GOOGLE", "ROLE_USER");
        String refreshToken = jwtProvider.createRefreshToken("google-sub", "GOOGLE");

        assertThat(jwtProvider.isAccessTokenValid(accessToken)).isTrue();
        assertThat(jwtProvider.isRefreshTokenValid(accessToken)).isFalse();
        assertThat(jwtProvider.isRefreshTokenValid(refreshToken)).isTrue();
        assertThat(jwtProvider.isAccessTokenValid(refreshToken)).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "UNKNOWN", "access", "refresh"})
    @DisplayName("토큰 용도가 없거나 알 수 없으면 두 용도 모두 거부한다.")
    void rejectMissingOrUnknownTokenType(String tokenType) {
        String token = Jwts.builder()
                .subject("google-sub")
                .claim("provider", "GOOGLE")
                .claim("role", "ROLE_USER")
                .claim("tokenType", tokenType)
                .signWith(Keys.hmacShaKeyFor("5f7e12f7-a7fa-4cf3-8831-7656034e90d3".getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtProvider.isTokenValid(token)).isTrue();
        assertThat(jwtProvider.isAccessTokenValid(token)).isFalse();
        assertThat(jwtProvider.isRefreshTokenValid(token)).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰은 용도가 맞아도 거부한다.")
    void rejectExpiredTokensForTheirPurpose() {
        JwtProvider expiredJwtProvider = new JwtProvider(
                "5f7e12f7-a7fa-4cf3-8831-7656034e90d3", 0L, 0L);

        assertThat(jwtProvider.isAccessTokenValid(
                expiredJwtProvider.createAccessToken("google-sub", "GOOGLE", "ROLE_USER"))).isFalse();
        assertThat(jwtProvider.isRefreshTokenValid(
                expiredJwtProvider.createRefreshToken("google-sub", "GOOGLE"))).isFalse();
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰은 용도가 맞아도 거부한다.")
    void rejectWrongSignatureForBothPurposes() {
        JwtProvider otherJwtProvider = new JwtProvider(
                "11111111-1111-1111-1111-111111111111", 1800000L, 604800000L);

        assertThat(jwtProvider.isAccessTokenValid(
                otherJwtProvider.createAccessToken("google-sub", "GOOGLE", "ROLE_USER"))).isFalse();
        assertThat(jwtProvider.isRefreshTokenValid(
                otherJwtProvider.createRefreshToken("google-sub", "GOOGLE"))).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "invalid.token.value"})
    @DisplayName("잘못된 입력은 두 용도 모두 거부한다.")
    void rejectInvalidInputForBothPurposes(String token) {
        assertThat(jwtProvider.isAccessTokenValid(token)).isFalse();
        assertThat(jwtProvider.isRefreshTokenValid(token)).isFalse();
    }

    //@Autowired 대신 직접 넣어주는 방식 (트레이드 오프를 감안하여 테스트에서는 다음과 같이 진행)
    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(
                "5f7e12f7-a7fa-4cf3-8831-7656034e90d3",
                1800000L,
                604800000L
        );
    }

    @Test
    @DisplayName("AccessToken을 생성할 수 있다.")
    void createAccessToken() {
        String socialId = "google-sub";
        String role = "ROLE_USER";

        String token = jwtProvider.createAccessToken(socialId, "GOOGLE", role);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    @DisplayName("RefreshToken을 생성할 수 있다.")
    void createRefreshToken() {
        // given
        String socialId = "google-sub";

        // when
        String token = jwtProvider.createRefreshToken(socialId, "GOOGLE");

        // then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    @DisplayName("같은 소셜 식별자로 연속 생성한 RefreshToken은 서로 달라야 한다.")
    void refreshTokenShouldBeUnique() {
        String socialId = "google-sub";

        String firstToken = jwtProvider.createRefreshToken(socialId, "GOOGLE");
        String secondToken = jwtProvider.createRefreshToken(socialId, "GOOGLE");

        assertThat(firstToken).isNotEqualTo(secondToken);
    }

    @Test
    @DisplayName("AccessToken에서 provider와 socialId를 추출할 수 있다.")
    void extractSocialIdentityFromToken() {
        // given
        String socialId = "google-sub";
        String token = jwtProvider.createAccessToken(socialId, "GOOGLE", "ROLE_USER");

        // when
        String extractedSocialId = jwtProvider.extractSocialId(token);

        // then
        assertThat(extractedSocialId).isEqualTo(socialId);
        assertThat(jwtProvider.extractProvider(token)).isEqualTo("GOOGLE");
    }

    @Test
    @DisplayName("RefreshToken에서 provider와 socialId를 추출할 수 있다.")
    void extractSocialIdentityFromRefreshToken() {
        // given
        String socialId = "google-sub";
        String token = jwtProvider.createRefreshToken(socialId, "GOOGLE");

        // when
        String extractedSocialId = jwtProvider.extractSocialId(token);

        // then
        assertThat(extractedSocialId).isEqualTo(socialId);
        assertThat(jwtProvider.extractProvider(token)).isEqualTo("GOOGLE");
    }

    @Test
    @DisplayName("유효한 토큰은 검증을 통과한다.")
    void validTokenPassesValidation() {
        // given
        String token = jwtProvider.createAccessToken("google-sub", "GOOGLE", "ROLE_USER");

        // when
        boolean isValid = jwtProvider.isTokenValid(token);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("만료된 토큰은 유효하지 않다.")
    void expiredTokenIsInvalid() {
        // given: 만료시간 0으로 즉시 만료
        JwtProvider expiredJwtProvider = new JwtProvider(
                "5f7e12f7-a7fa-4cf3-8831-7656034e90d3",
                0L,
                0L
        );
        String token = expiredJwtProvider.createAccessToken("google-sub", "GOOGLE", "ROLE_USER");

        // when
        boolean isValid = jwtProvider.isTokenValid(token);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("잘못된 토큰은 유효하지 않다.")
    void invalidTokenIsInvalid() {
        // when
        boolean isValid = jwtProvider.isTokenValid("invalid.token.value");

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("AccessToken에서 role을 추출할 수 있다.")
    void extractRoleFromAccessToken() {
        // given
        String role = "ROLE_USER";
        String token = jwtProvider.createAccessToken("google-sub", "GOOGLE", role);

        // when
        String extractedRole = jwtProvider.extractRole(token);

        // then
        assertThat(extractedRole).isEqualTo(role);
    }

    @Test
    @DisplayName("RefreshToken에서 role을 추출할 수 없다.")
    void canNotExtractRoleFromRefreshToken() {
        String token = jwtProvider.createRefreshToken("google-sub", "GOOGLE");

        String extractedRole = jwtProvider.extractRole(token);

        assertThat(extractedRole).isNull();

    }

    @Test
    @DisplayName("만료된 RefreshToken은 유효하지 않다.")
    void expiredRefreshTokenIsInvalid() {
        // given
        JwtProvider expiredJwtProvider = new JwtProvider(
                "5f7e12f7-a7fa-4cf3-8831-7656034e90d3",
                1800000L,
                0L
        );
        String token = expiredJwtProvider.createRefreshToken("google-sub", "GOOGLE");

        // when
        boolean isValid = jwtProvider.isTokenValid(token);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("다른 secret key로 서명된 토큰은 유효하지 않다.")
    void tokenSignedWithDifferentKeyIsInvalid() {
        // given
        JwtProvider otherJwtProvider = new JwtProvider(
                "11111111-1111-1111-1111-111111111111",
                1800000L,
                604800000L
        );
        String token = otherJwtProvider.createAccessToken("google-sub", "GOOGLE", "ROLE_USER");

        // when
        boolean isValid = jwtProvider.isTokenValid(token);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("JWT는 NULL값이거나 비워져 있으면 안된다.")
    void tokenCanNotBeNull(){
        boolean isValidEmpty = jwtProvider.isTokenValid("");
        boolean isValidNull = jwtProvider.isTokenValid(null);

        assertThat(isValidEmpty).isFalse();
        assertThat(isValidNull).isFalse();
    }
}
