package com.stardy.user.controller;

import com.stardy.user.dto.DomainUpdateRequestDto;
import com.stardy.user.dto.MessageResponseDto;
import com.stardy.user.dto.NicknameCheckResponseDto;
import com.stardy.user.dto.NicknameUpdateRequestDto;
import com.stardy.user.dto.ProfileImageListResponseDto;
import com.stardy.user.dto.ProfileImageUpdateRequestDto;
import com.stardy.user.dto.UserDto;
import com.stardy.user.exception.BaseException;
import com.stardy.user.global.JwtProvider;
import com.stardy.user.service.AuthService;
import com.stardy.user.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.Duration;

import static com.stardy.user.exception.ErrorCode.UNAUTHORIZED;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String AUTH_COOKIE_PATH = "/api/v1/auth";

    private final UserService userService;
    private final AuthService authService;
    private final JwtProvider jwtProvider;

    public UserController(UserService userService, AuthService authService, JwtProvider jwtProvider) {
        this.userService = userService;
        this.authService = authService;
        this.jwtProvider = jwtProvider;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getMyInfo(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        String email = extractEmail(authorization);

        return ResponseEntity.ok(userService.getUserInfo(email));
    }

    @GetMapping("/nickname/check")
    public ResponseEntity<NicknameCheckResponseDto> checkNickname(@RequestParam String value) {
        boolean available = userService.isNicknameAvailable(value);

        return ResponseEntity.ok(new NicknameCheckResponseDto(available));
    }

    @GetMapping("/profile-images")
    public ResponseEntity<ProfileImageListResponseDto> getProfileImages() {
        List<String> images = userService.getProfileImageList();

        return ResponseEntity.ok(ProfileImageListResponseDto.from(images));
    }

    @PatchMapping("/me/nickname")
    public ResponseEntity<MessageResponseDto> changeNickname(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody NicknameUpdateRequestDto request
    ) {
        userService.changeNickname(extractEmail(authorization), request.nickname());
        return ResponseEntity.ok(new MessageResponseDto("닉네임이 변경되었습니다."));
    }

    @PatchMapping("/me/profile-image")
    public ResponseEntity<MessageResponseDto> changeProfileImage(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody ProfileImageUpdateRequestDto request
    ) {
        userService.changeProfileImage(extractEmail(authorization), request.profileImageUrl());
        return ResponseEntity.ok(new MessageResponseDto("프로필 이미지가 변경되었습니다."));
    }

    @PatchMapping("/me/domain")
    public ResponseEntity<MessageResponseDto> changeDomain(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody DomainUpdateRequestDto request
    ) {
        userService.changeDomain(extractEmail(authorization), request.domain());
        return ResponseEntity.ok(new MessageResponseDto("관심 지역 및 과목이 변경되었습니다."));
    }

    @DeleteMapping("/me")
    public ResponseEntity<MessageResponseDto> deleteUser(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        String email = extractEmail(authorization);
        userService.deleteUser(email);
        authService.logout(email);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expireRefreshTokenCookie().toString())
                .body(new MessageResponseDto("회원 탈퇴가 완료되었습니다."));
    }

    private String extractEmail(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BaseException(UNAUTHORIZED);
        }

        String token = authorization.substring(BEARER_PREFIX.length());
        if (token.isBlank() || !jwtProvider.isTokenValid(token)) {
            throw new BaseException(UNAUTHORIZED);
        }

        return jwtProvider.extractEmail(token);
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
