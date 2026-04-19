package com.stardy.user.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("BaseException은 ErrorCode에 맞는 상태코드와 응답 본문으로 변환한다.")
    void handleBaseException() {
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBaseException(new InvalidTokenException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_TOKEN");
        assertThat(response.getBody().message()).isEqualTo("유효하지 않은 토큰입니다");
    }

    @Test
    @DisplayName("필수 파라미터 누락은 MISSING_PARAMETER로 변환한다.")
    void handleMissingParameterException() {
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMissingParameterException(
                new MissingServletRequestParameterException("refreshToken", "String")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("MISSING_PARAMETER");
    }

    @Test
    @DisplayName("처리되지 않은 예외는 SERVER_ERROR로 변환한다.")
    void handleException() {
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("SERVER_ERROR");
    }
}
