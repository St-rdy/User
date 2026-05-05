package com.stardy.user.global;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stardy.user.exception.BaseException;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.stardy.user.exception.ErrorCode.INVALID_DOMAIN;

@Component
public class DomainValidator {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<String> allowedRegions = new HashSet<>();
    private final Set<String> allowedSubjects = new HashSet<>();

    @PostConstruct
    public void loadDomains() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/domain.json");
        Map<String, List<Map<String, String>>> domainOptions = objectMapper.readValue(
                resource.getInputStream(),
                new TypeReference<>() {
                }
        );

        loadValues(domainOptions.get("regions"), allowedRegions);
        loadValues(domainOptions.get("subjects"), allowedSubjects);
    }

    public void validate(Map<String, Object> domain) {
        if (domain == null || !domain.containsKey("regions") || !domain.containsKey("subjects")) {
            throw new BaseException(INVALID_DOMAIN);
        }

        validateValues(domain.get("regions"), allowedRegions);
        validateValues(domain.get("subjects"), allowedSubjects);
    }

    private void loadValues(List<Map<String, String>> options, Set<String> target) {
        if (options == null) {
            return;
        }

        for (Map<String, String> option : options) {
            String value = option.get("value");
            if (value != null && !value.isBlank()) {
                target.add(value);
            }
        }
    }

    private void validateValues(Object values, Set<String> allowedValues) {
        if (!(values instanceof Collection<?> requestedValues) || requestedValues.isEmpty()) {
            throw new BaseException(INVALID_DOMAIN);
        }

        for (Object value : requestedValues) {
            if (!(value instanceof String stringValue) || !allowedValues.contains(stringValue)) {
                throw new BaseException(INVALID_DOMAIN);
            }
        }
    }
}
