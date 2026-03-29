package com.stardy.user;

import com.stardy.user.entity.User;
import com.stardy.user.repository.UserRepository;
import com.stardy.user.service.AuthService;
import com.stardy.user.repository.RedisTokenRepository;
import com.stardy.user.dto.TokenResponse;
import com.stardy.user.config.TestContainersConfig;
import com.stardy.user.exception.InvalidTokenException;
import com.stardy.user.global.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
class UserApplicationTests {
    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RedisTokenRepository redisTokenRepository;
    @Autowired
    private JwtProvider jwtProvider;


    @Test
    void contextLoads() {
    }

    @Test
    @DisplayName("유효한 RefreshToken으로 AccessToken과 RefreshToken을 재발급한다.")
    void reissueTokens() {
        //1. 로그인 되어 있는 상황을 만들기 위함, reissueToken 에서 UserDB를 조회하기 때문에 DB 저장 로직 추가
        String email = "test@gmail.com";
        String role = "ROLE_USER";
        userRepository.save(new User(email, role));

        String oldRefreshToken = jwtProvider.createRefreshToken(email);
        redisTokenRepository.saveRefreshToken(email, oldRefreshToken);

        //2. 재발급 로직
        TokenResponse result = authService.reissueToken(oldRefreshToken);

        //3. 재발급된 토큰이 유효함.
        assertThat(result.getAccessToken()).isNotNull();
        assertThat(result.getRefreshToken()).isNotNull();
        assertThat(result.getRefreshToken()).isNotEqualTo(oldRefreshToken);
    }

    @Test
    @DisplayName("유효하지 않은 RefreshToken으로 재발급 시 예외를 던진다.")
    void reissueWithInvalidToken() {
        String invalidToken = "invalid-token";

        assertThatThrownBy(() -> authService.reissueToken(invalidToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("유효하지 않은 토큰입니다");
    }

    @Test
    @DisplayName("Redis에 저장된 토큰과 다르면 예외를 던진다. (토큰 탈취 방지)")
    void reissueWithStolenToken() {
        //1. 로그인 되어 있는 상황을 만들기 위함, reissueToken 에서 UserDB를 조회하기 때문에 DB 저장 로직 추가
        String email = "test@gmail.com";
        String role = "ROLE_USER";
        String firstRefreshToken = jwtProvider.createRefreshToken(email);
        redisTokenRepository.saveRefreshToken(email, firstRefreshToken);
        userRepository.save(new User(email, role));

        //2. RTR 방식으로 RT 재발급 (사용자가 정상적으로 변경)
        authService.reissueToken(firstRefreshToken);

        //3. firstRefreshToken 과 secondRefreshToken이 다르니 예외 발생
        assertThatThrownBy(() -> authService.reissueToken(firstRefreshToken))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("Redis에 저장된 RefreshToken이 없으면 예외를 던진다.")
    void reissueWithNoStoredToken() {
        //1. 로그인 되어 있는 상황을 만들기 위함. 하지만 Redis 에 저장하지 않음
        String email = "test@gmail.com";
        String refreshToken = jwtProvider.createRefreshToken(email);

        //2. refreshToken이 redis에 저장되있지 않기 때문에 예외 발생
        assertThatThrownBy(() -> authService.reissueToken(refreshToken))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("로그아웃 시 Redis에서 RefreshToken을 삭제한다.")
    void logout() {
        // 1. 로그인 되어 있는 상황을 만들기 위함.
        String email = "test@gmail.com";
        String refreshToken = jwtProvider.createRefreshToken(email);
        redisTokenRepository.saveRefreshToken(email, refreshToken);

        // 2. 로그아웃 기능
        authService.logout(email);

        //3. Redis에서 Email을 가지고 검증하면 null 이어야 함.
        assertThat(redisTokenRepository.getRefreshToken(email)).isNull();
    }
}
