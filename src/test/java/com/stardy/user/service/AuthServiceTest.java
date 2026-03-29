package com.stardy.user.service;

import com.stardy.user.dto.TokenResponse;
import com.stardy.user.exception.InvalidTokenException;
import com.stardy.user.global.JwtProvider;
import com.stardy.user.repository.RedisTokenRepository;
import com.stardy.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/*
1. RefreshToken으로 AccessToken 재발급 (+ RefreshToken도 재발급)
2. RefreshToken 유효성 검증
3. Redis에 저장된 토큰과 일치하는지 확인 (토큰 탈취 방지)
4. 로그아웃 시 Redis에서 RefreshToken 삭제

@author Jinwook Jung
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RedisTokenRepository redisTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("유효한 RefreshToken으로 AccessToken과 RefreshToken을 재발급한다.")
    void reissueTokens() {
        // given
        String oldRefreshToken = "old-refresh-token";
        String email = "test@gmail.com";
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";
        String role = "ROLE_USER";

        given(jwtProvider.isTokenValid(oldRefreshToken)).willReturn(true);
        given(jwtProvider.extractEmail(oldRefreshToken)).willReturn(email);

        given(redisTokenRepository.getRefreshToken(email)).willReturn(oldRefreshToken);
        given(userRepository.getRole(email)).willReturn(role);

        given(jwtProvider.createAccessToken(eq(email), eq(role))).willReturn(newAccessToken);
        given(jwtProvider.createRefreshToken(email)).willReturn(newRefreshToken);

        // when
        TokenResponse result = authService.reissueToken(oldRefreshToken);

        // then
        assertThat(result.getAccessToken()).isEqualTo(newAccessToken);
        assertThat(result.getRefreshToken()).isEqualTo(newRefreshToken);
        then(redisTokenRepository).should().deleteRefreshToken(email);
        then(redisTokenRepository).should().saveRefreshToken(email, newRefreshToken);
    }

    @Test
    @DisplayName("유효하지 않은 RefreshToken으로 재발급 시 예외를 던진다.")
    void reissueWithInvalidToken() {
        // given
        String invalidToken = "invalid-token";
        given(jwtProvider.isTokenValid(invalidToken)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.reissueToken(invalidToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("유효하지 않은 토큰입니다");
    }

    @Test
    @DisplayName("Redis에 저장된 토큰과 다르면 예외를 던진다. (토큰 탈취 방지)")
    void reissueWithStolenToken() {
        // given
        String stolenToken = "stolen-token";
        String email = "test@gmail.com";
        String storedToken = "real-refresh-token";

        given(jwtProvider.isTokenValid(stolenToken)).willReturn(true);
        given(jwtProvider.extractEmail(stolenToken)).willReturn(email);
        given(redisTokenRepository.getRefreshToken(email)).willReturn(storedToken);

        // when & then
        assertThatThrownBy(() -> authService.reissueToken(stolenToken))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("Redis에 저장된 RefreshToken이 없으면 예외를 던진다.")
    void reissueWithNoStoredToken() {
        // given
        String refreshToken = "refresh-token";
        String email = "test@gmail.com";

        given(jwtProvider.isTokenValid(refreshToken)).willReturn(true);
        given(jwtProvider.extractEmail(refreshToken)).willReturn(email);
        given(redisTokenRepository.getRefreshToken(email)).willReturn(null); // Redis에 없는 경우

        // when & then
        assertThatThrownBy(() -> authService.reissueToken(refreshToken))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("로그아웃 시 Redis에서 RefreshToken을 삭제한다.")
    void logout() {
        // given
        String email = "test@gmail.com";

        // when
        authService.logout(email);

        // then
        then(redisTokenRepository).should().deleteRefreshToken(email);
    }
}
