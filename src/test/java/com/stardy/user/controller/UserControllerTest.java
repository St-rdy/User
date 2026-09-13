package com.stardy.user.controller;

import com.stardy.user.dto.UserDto;
import com.stardy.user.exception.GlobalExceptionHandler;
import com.stardy.user.global.JwtProvider;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        UserController userController = new UserController(userService, jwtProvider);

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
}
