package com.stardy.user.global;

import com.stardy.user.exception.BaseException;
import com.stardy.user.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomainValidatorTest {

    @Test
    @DisplayName("도메인 JSON에 존재하는 지역과 과목은 사용할 수 있다.")
    void validate() throws IOException {
        DomainValidator domainValidator = new DomainValidator();
        domainValidator.loadDomains();
        Map<String, Object> domain = Map.of(
                "regions", List.of("Seoul"),
                "subjects", List.of("English")
        );

        assertDoesNotThrow(() -> domainValidator.validate(domain));
    }

    @Test
    @DisplayName("도메인 JSON에 없는 지역은 사용할 수 없다.")
    void validateWithNotAllowedRegion() throws IOException {
        DomainValidator domainValidator = new DomainValidator();
        domainValidator.loadDomains();
        Map<String, Object> domain = Map.of(
                "regions", List.of("America"),
                "subjects", List.of("English")
        );

        BaseException exception = assertThrows(BaseException.class, () -> domainValidator.validate(domain));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_DOMAIN);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.INVALID_DOMAIN.getMessage());
    }

    @Test
    @DisplayName("도메인 JSON에 없는 과목은 사용할 수 없다.")
    void validateWithNotAllowedSubject() throws IOException {
        DomainValidator domainValidator = new DomainValidator();
        domainValidator.loadDomains();
        Map<String, Object> domain = Map.of(
                "regions", List.of("Seoul"),
                "subjects", List.of("Hacking")
        );

        BaseException exception = assertThrows(BaseException.class, () -> domainValidator.validate(domain));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_DOMAIN);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.INVALID_DOMAIN.getMessage());
    }

    @Test
    @DisplayName("필수 도메인 key가 없으면 사용할 수 없다.")
    void validateWithMissingKey() throws IOException {
        DomainValidator domainValidator = new DomainValidator();
        domainValidator.loadDomains();
        Map<String, Object> domain = Map.of(
                "regions", List.of("Seoul")
        );

        BaseException exception = assertThrows(BaseException.class, () -> domainValidator.validate(domain));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_DOMAIN);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.INVALID_DOMAIN.getMessage());
    }
}
