package com.stardy.user.global;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class BadWordFilterTest {

    @Test
    @DisplayName("비속어 테스트")
    void containsBadWord() throws IOException {
        BadWordFilter badWordFilter = new BadWordFilter();
        badWordFilter.loadBadWords();

        assertThat(badWordFilter.containsBadWord("씨발")).isTrue();
        assertThat(badWordFilter.containsBadWord("개씨발")).isTrue();
        assertThat(badWordFilter.containsBadWord("씨@발")).isTrue();          // 정규식으로 해결
        assertThat(badWordFilter.containsBadWord("쓔발")).isTrue();
        assertThat(badWordFilter.containsBadWord("개쓔발")).isTrue();
        assertThat(badWordFilter.containsBadWord("정상 닉네임")).isFalse();

    }
}
