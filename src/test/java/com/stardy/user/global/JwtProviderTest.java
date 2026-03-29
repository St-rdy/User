package com.stardy.user.global;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/*
생성  →  createAccessToken(), createRefreshToken()
검증  →  isTokenValid()
추출  →  extractEmail(), extractRole()

@author Jinwook Jung
 */
class JwtProviderTest {
    private JwtProvider jwtProvider;

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
        String email = "test@gmail.com";
        String role = "ROLE_USER";

        String token = jwtProvider.createAccessToken(email, role);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    @DisplayName("RefreshToken을 생성할 수 있다.")
    void createRefreshToken() {
        // given
        String email = "test@gmail.com";

        // when
        String token = jwtProvider.createRefreshToken(email);

        // then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    @DisplayName("AccessToken에서 이메일을 추출할 수 있다.")
    void extractEmailFromToken() {
        // given
        String email = "test@gmail.com";
        String token = jwtProvider.createAccessToken(email, "ROLE_USER");

        // when
        String extractedEmail = jwtProvider.extractEmail(token);

        // then
        assertThat(extractedEmail).isEqualTo(email);
    }

    @Test
    @DisplayName("RefreshToken에서 이메일을 추출할 수 있다.")
    void extractEmailFromRefreshToken() {
        // given
        String email = "test@gmail.com";
        String token = jwtProvider.createRefreshToken(email);

        // when
        String extractedEmail = jwtProvider.extractEmail(token);

        // then
        assertThat(extractedEmail).isEqualTo(email);
    }

    @Test
    @DisplayName("유효한 토큰은 검증을 통과한다.")
    void validTokenPassesValidation() {
        // given
        String token = jwtProvider.createAccessToken("test@gmail.com", "ROLE_USER");

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
        String token = expiredJwtProvider.createAccessToken("test@gmail.com", "ROLE_USER");

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
        String token = jwtProvider.createAccessToken("test@gmail.com", role);

        // when
        String extractedRole = jwtProvider.extractRole(token);

        // then
        assertThat(extractedRole).isEqualTo(role);
    }

    @Test
    @DisplayName("RefreshToken에서 role을 추출할 수 없다.")
    void canNotExtractRoleFromRefreshToken() {
        String token = jwtProvider.createRefreshToken("test@gmail.com");

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
        String token = expiredJwtProvider.createRefreshToken("test@gmail.com");

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
        String token = otherJwtProvider.createAccessToken("test@gmail.com", "ROLE_USER");

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
