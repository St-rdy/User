package com.stardy.user.global;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
public class BadWordFilter {
    private final Set<String> badWords = new HashSet<>();

    @PostConstruct
    public void loadBadWords() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/badwords.txt");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    badWords.add(normalize(line.trim()));
                }
            }
        }
    }

    public boolean containsBadWord(String nickname) {
        String normalizedNickname = normalize(nickname);

        return badWords.stream()
                .anyMatch(normalizedNickname::contains);
    }

    private String normalize(String text) {
        return text
                .replaceAll("[^가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9]", "") // 특수문자, 공백 제거
                .toLowerCase();
    }
}
