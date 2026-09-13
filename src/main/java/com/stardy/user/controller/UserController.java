package com.stardy.user.controller;

import com.stardy.user.dto.NicknameCheckResponseDto;
import com.stardy.user.dto.ProfileImageListResponseDto;
import com.stardy.user.dto.UserDto;
import com.stardy.user.exception.BaseException;
import com.stardy.user.global.JwtProvider;
import com.stardy.user.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.stardy.user.exception.ErrorCode.UNAUTHORIZED;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserService userService;
    private final JwtProvider jwtProvider;

    public UserController(UserService userService, JwtProvider jwtProvider) {
        this.userService = userService;
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

}
