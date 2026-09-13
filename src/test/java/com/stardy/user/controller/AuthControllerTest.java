package com.stardy.user.controller;

import com.stardy.user.dto.TokenResponseDto;
import com.stardy.user.exception.BaseException;
import com.stardy.user.exception.ErrorCode;
import com.stardy.user.exception.GlobalExceptionHandler;
import com.stardy.user.global.JwtProvider;
import com.stardy.user.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final long ACCESS_TOKEN_EXPIRATION = 1_800_000L;
    private static final long REFRESH_TOKEN_EXPIRATION = 604_800_000L;

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(
                authService,
                jwtProvider,
                ACCESS_TOKEN_EXPIRATION,
                REFRESH_TOKEN_EXPIRATION
        );

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("임시 코드로 Access Token을 발급하고 Refresh Token은 Cookie로 내려준다.")
    void exchangeTemporaryCode() throws Exception {
        String tempCode = "550e8400-e29b-41d4-a716-446655440000";
        given(authService.exchangeTemporaryCode(tempCode))
                .willReturn(new TokenResponseDto("access-token", "refresh-token"));

        mockMvc.perform(get("/api/v1/auth/token")
                        .param("code", tempCode))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=refresh-token")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/v1/auth")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800));
    }

    @Test
    @DisplayName("임시 코드가 없으면 MISSING_PARAMETER를 반환한다.")
    void exchangeTemporaryCodeWithoutCode() throws Exception {
        mockMvc.perform(get("/api/v1/auth/token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"));
    }

    @Test
    @DisplayName("만료되거나 사용된 임시 코드는 INVALID_TEMP_CODE를 반환한다.")
    void exchangeTemporaryCodeWithInvalidCode() throws Exception {
        String tempCode = "expired-code";
        given(authService.exchangeTemporaryCode(tempCode))
                .willThrow(new BaseException(ErrorCode.INVALID_TEMP_CODE));

        mockMvc.perform(get("/api/v1/auth/token")
                        .param("code", tempCode))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TEMP_CODE"));
    }

    @Test
    @DisplayName("Refresh Token Cookie로 Access Token과 새 Refresh Token을 재발급한다.")
    void reissueToken() throws Exception {
        given(authService.reissueToken("old-refresh-token"))
                .willReturn(new TokenResponseDto("new-access-token", "new-refresh-token"));

        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=new-refresh-token")))
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800));
    }

    @Test
    @DisplayName("Refresh Token Cookie가 없으면 REFRESH_TOKEN_NOT_FOUND를 반환한다.")
    void reissueTokenWithoutRefreshToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/token/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_NOT_FOUND"));
    }

    @Test
    @DisplayName("유효한 Access Token으로 로그아웃하고 Refresh Token Cookie를 만료한다.")
    void logout() throws Exception {
        String accessToken = "access-token";
        given(jwtProvider.isTokenValid(accessToken)).willReturn(true);
        given(jwtProvider.extractEmail(accessToken)).willReturn("test@gmail.com");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andExpect(jsonPath("$.message").value("로그아웃이 완료되었습니다."));

        then(authService).should().logout("test@gmail.com");
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 로그아웃할 수 없다.")
    void logoutWithoutAuthorization() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
