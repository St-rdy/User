package com.stardy.user.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_TEMP_CODE(HttpStatus.BAD_REQUEST, "INVALID_TEMP_CODE", "임시 코드가 유효하지 않습니다."),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", "필수 파라미터가 누락되었습니다."),
    INVALID_PROVIDER(HttpStatus.BAD_REQUEST, "INVALID_PROVIDER", "지원하지 않는 provider입니다."),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "INVALID_NICKNAME", "유효하지 않은 닉네임 형식입니다."),
    INVALID_PROFILE_IMAGE(HttpStatus.BAD_REQUEST, "INVALID_PROFILE_IMAGE", "허용되지 않은 프로필 이미지 URL입니다."),
    LAST_PROVIDER(HttpStatus.BAD_REQUEST, "LAST_PROVIDER", "최소 1개의 소셜 연동이 필요합니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증 정보가 유효하지 않습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "토큰이 만료되었습니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_NOT_FOUND", "Refresh Token이 존재하지 않습니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED", "Refresh Token이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    PROVIDER_NOT_FOUND(HttpStatus.NOT_FOUND, "PROVIDER_NOT_FOUND", "연결된 소셜 계정을 찾을 수 없습니다."),
    OAUTH2_AUTH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OAUTH2_AUTH_FAILED", "소셜 인증에 실패했습니다."),
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
