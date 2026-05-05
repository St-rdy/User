package com.stardy.user.service;

import com.stardy.user.dto.TokenResponseDto;
import com.stardy.user.entity.Role;
import com.stardy.user.entity.User;
import com.stardy.user.exception.BaseException;
import com.stardy.user.exception.ErrorCode;
import com.stardy.user.global.JwtProvider;
import com.stardy.user.repository.RedisTokenRepository;
import com.stardy.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
        given(userRepository.findByEmail(email)).willReturn(Optional.of(createUserWithRole(email, role)));

        given(jwtProvider.createAccessToken(eq(email), eq(role))).willReturn(newAccessToken);
        given(jwtProvider.createRefreshToken(email)).willReturn(newRefreshToken);

        // when
        TokenResponseDto result = authService.reissueToken(oldRefreshToken);

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

        // when
        BaseException exception = assertThrows(BaseException.class, () -> authService.reissueToken(invalidToken));

        // then
        assertBaseException(exception, ErrorCode.INVALID_TOKEN);
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

        // when
        BaseException exception = assertThrows(BaseException.class, () -> authService.reissueToken(stolenToken));

        // then
        assertBaseException(exception, ErrorCode.INVALID_TOKEN);
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

        // when
        BaseException exception = assertThrows(BaseException.class, () -> authService.reissueToken(refreshToken));

        // then
        assertBaseException(exception, ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("회원 정보가 없으면 예외를 던진다.")
    void reissueWithNoUser() {
        String refreshToken = "refresh-token";
        String email = "test@gmail.com";

        given(jwtProvider.isTokenValid(refreshToken)).willReturn(true);
        given(jwtProvider.extractEmail(refreshToken)).willReturn(email);
        given(redisTokenRepository.getRefreshToken(email)).willReturn(refreshToken);
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        BaseException exception = assertThrows(BaseException.class, () -> authService.reissueToken(refreshToken));

        assertBaseException(exception, ErrorCode.INVALID_TOKEN);
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

    @Test
    @DisplayName("OAuth2 로그인 성공 시 임시 코드(UUID)를 발급하고 Redis에 저장한다.")
    void issueTemporaryCode() {
        // given
        String email = "test@gmail.com";
        String role = "ROLE_USER";
        String fakeAccessToken = "fake-access-token";
        String fakeRefreshToken = "fake-refresh-token";

        // JWT 발급은 JwtProvider가 담당 — Mock으로 가짜 토큰 반환
        given(jwtProvider.createAccessToken(eq(email), eq(role))).willReturn(fakeAccessToken);
        given(jwtProvider.createRefreshToken(email)).willReturn(fakeRefreshToken);

        // when
        String tempCode = authService.issueTemporaryCode(email, role);

        // then
        // UUID 형식인지 검증
        assertThat(tempCode).isNotNull();
        assertThat(tempCode).matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
        );
        // Redis에 임시 코드가 저장됐는지 검증
        then(redisTokenRepository).should().saveTemporaryCode(eq(tempCode), any(TokenResponseDto.class));
    }

    @Test
    @DisplayName("유효한 임시 코드로 교환 시 TokenResponse를 반환하고 Redis에서 삭제한다.")
    void exchangeTemporaryCode() {
        // given
        String tempCode = UUID.randomUUID().toString();
        TokenResponseDto expectedTokens = new TokenResponseDto("access-token", "refresh-token");

        // Redis에서 임시 코드를 조회하면 TokenResponse를 반환하도록 설정
        given(redisTokenRepository.getTemporaryCode(tempCode)).willReturn(expectedTokens);

        // when
        TokenResponseDto result = authService.exchangeTemporaryCode(tempCode);

        // then
        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        // 일회성 코드이므로 사용 즉시 삭제됐는지 검증
        then(redisTokenRepository).should().deleteTemporaryCode(tempCode);
    }

    @Test
    @DisplayName("이미 사용된 임시 코드로 교환 시 예외를 던진다.")
    void exchangeTemporaryCodeAlreadyUsed() {
        // given
        String usedCode = UUID.randomUUID().toString();

        // Redis에 없는 상태 — 이미 사용되어 삭제된 것을 시뮬레이션
        given(redisTokenRepository.getTemporaryCode(usedCode)).willReturn(null);

        // when
        BaseException exception = assertThrows(BaseException.class, () -> authService.exchangeTemporaryCode(usedCode));

        // then
        assertBaseException(exception, ErrorCode.INVALID_TEMP_CODE);
    }

    @Test
    @DisplayName("존재하지 않는 임시 코드로 교환 시 예외를 던진다.")
    void exchangeTemporaryCodeNotFound() {
        // given
        String fakeCode = "not-exist-code";
        given(redisTokenRepository.getTemporaryCode(fakeCode)).willReturn(null);

        // when
        BaseException exception = assertThrows(BaseException.class, () -> authService.exchangeTemporaryCode(fakeCode));

        // then
        assertBaseException(exception, ErrorCode.INVALID_TEMP_CODE);
    }

    private User createUserWithRole(String email, String roleName) {
        User user = new User(
                email,
                "Test User",
                "test-nickname",
                "https://example.com/profile.png",
                Map.of("domains", List.of("backend")),
                new Role(roleName, roleDisplayName(roleName)),
                "ACTIVE"
        );
        return user;
    }

    private String roleDisplayName(String roleId) {
        if ("ROLE_ADMIN".equals(roleId)) {
            return "관리자";
        }
        return "일반 사용자";
    }

    private void assertBaseException(BaseException exception, ErrorCode errorCode) {
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(errorCode.getMessage());
    }
}
