package com.stardy.user.controller;

import com.stardy.user.dto.UserDto;
import com.stardy.user.exception.GlobalExceptionHandler;
import com.stardy.user.global.JwtProvider;
import com.stardy.user.service.AuthService;
import com.stardy.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    @Mock
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        UserController userController = new UserController(userService, authService, jwtProvider);

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Access Token으로 내 정보를 조회한다.")
    void getMyInfo() throws Exception {
        String accessToken = "access-token";
        String email = "test@gmail.com";
        UserDto user = new UserDto(
                1L,
                email,
                "Test User",
                "test-nickname",
                "https://example.com/profile.png",
                Map.of("regions", List.of("Seoul"), "subjects", List.of("Math")),
                "ACTIVE",
                "ROLE_USER"
        );

        given(jwtProvider.isTokenValid(accessToken)).willReturn(true);
        given(jwtProvider.extractEmail(accessToken)).willReturn(email);
        given(userService.getUserInfo(email)).willReturn(user);

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.nickname").value("test-nickname"))
                .andExpect(jsonPath("$.roleId").value("ROLE_USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 내 정보를 조회할 수 없다.")
    void getMyInfoWithoutAuthorization() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("닉네임 사용 가능 여부를 조회한다.")
    void checkNickname() throws Exception {
        given(userService.isNicknameAvailable("newNick")).willReturn(true);

        mockMvc.perform(get("/api/v1/users/nickname/check")
                        .param("value", "newNick"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    @DisplayName("선택 가능한 프로필 이미지 목록을 조회한다.")
    void getProfileImages() throws Exception {
        given(userService.getProfileImageList()).willReturn(List.of(
                "https://example.com/profile-1.png",
                "https://example.com/profile-2.png"
        ));

        mockMvc.perform(get("/api/v1/users/profile-images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images[0].id").value(1))
                .andExpect(jsonPath("$.images[0].url").value("https://example.com/profile-1.png"))
                .andExpect(jsonPath("$.images[1].id").value(2))
                .andExpect(jsonPath("$.images[1].url").value("https://example.com/profile-2.png"));
    }

    @Test
    @DisplayName("내 닉네임을 변경한다.")
    void changeNickname() throws Exception {
        String accessToken = "access-token";
        given(jwtProvider.isTokenValid(accessToken)).willReturn(true);
        given(jwtProvider.extractEmail(accessToken)).willReturn("test@gmail.com");

        mockMvc.perform(patch("/api/v1/users/me/nickname")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("{\"nickname\":\"new-nickname\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("닉네임이 변경되었습니다."));

        then(userService).should().changeNickname("test@gmail.com", "new-nickname");
    }

    @Test
    @DisplayName("내 프로필 이미지를 변경한다.")
    void changeProfileImage() throws Exception {
        String accessToken = "access-token";
        given(jwtProvider.isTokenValid(accessToken)).willReturn(true);
        given(jwtProvider.extractEmail(accessToken)).willReturn("test@gmail.com");

        mockMvc.perform(patch("/api/v1/users/me/profile-image")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("{\"profileImageUrl\":\"https://example.com/new.png\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("프로필 이미지가 변경되었습니다."));

        then(userService).should().changeProfileImage("test@gmail.com", "https://example.com/new.png");
    }

    @Test
    @DisplayName("내 관심 지역과 과목을 변경한다.")
    void changeDomain() throws Exception {
        String accessToken = "access-token";
        given(jwtProvider.isTokenValid(accessToken)).willReturn(true);
        given(jwtProvider.extractEmail(accessToken)).willReturn("test@gmail.com");

        mockMvc.perform(patch("/api/v1/users/me/domain")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("{\"domain\":{\"regions\":[\"Seoul\"],\"subjects\":[\"Mathematics\"]}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("관심 지역 및 과목이 변경되었습니다."));

        then(userService).should().changeDomain(
                "test@gmail.com", Map.of("regions", List.of("Seoul"), "subjects", List.of("Mathematics"))
        );
    }

    @Test
    @DisplayName("회원 탈퇴 시 Refresh Token을 삭제한다.")
    void deleteUser() throws Exception {
        String accessToken = "access-token";
        given(jwtProvider.isTokenValid(accessToken)).willReturn(true);
        given(jwtProvider.extractEmail(accessToken)).willReturn("test@gmail.com");

        mockMvc.perform(delete("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("refreshToken=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")))
                .andExpect(jsonPath("$.message").value("회원 탈퇴가 완료되었습니다."));

        then(userService).should().deleteUser("test@gmail.com");
        then(authService).should().logout("test@gmail.com");
    }
}
