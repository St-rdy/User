package com.stardy.user.service;

import com.stardy.user.dto.OAuthSignupInfoDto;
import com.stardy.user.entity.UserProvider;
import com.stardy.user.exception.BaseException;
import com.stardy.user.repository.UserProviderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static com.stardy.user.exception.ErrorCode.OAUTH2_AUTH_FAILED;

@Service
public class NaverOAuth2Service {

    private static final String NAVER = "NAVER";
    private static final String DEFAULT_ROLE_ID = "ROLE_USER";

    private final UserProviderRepository userProviderRepository;

    public NaverOAuth2Service(UserProviderRepository userProviderRepository) {
        this.userProviderRepository = userProviderRepository;
    }

    @Transactional
    public GoogleOAuth2Service.OAuthLoginUser login(Map<String, Object> attributes) {
        Map<String, Object> profile = profile(attributes);
        String socialId = requiredAttribute(profile, "id");

        return userProviderRepository.findByProviderAndSocialId(NAVER, socialId)
                .map(this::existingUser)
                .orElseGet(() -> prepareSignup(profile, socialId));
    }

    private GoogleOAuth2Service.OAuthLoginUser prepareSignup(Map<String, Object> profile, String socialId) {
        String email = requiredAttribute(profile, "email");
        String name = optionalAttribute(profile, "name", optionalAttribute(profile, "nickname", email));

        return GoogleOAuth2Service.OAuthLoginUser.signup(
                new OAuthSignupInfoDto(email, name, DEFAULT_ROLE_ID, NAVER, socialId, email)
        );
    }

    private GoogleOAuth2Service.OAuthLoginUser existingUser(UserProvider userProvider) {
        if (userProvider.getUser().getRole() == null) {
            throw new BaseException(OAUTH2_AUTH_FAILED);
        }
        return new GoogleOAuth2Service.OAuthLoginUser(
                userProvider.getProvider(), userProvider.getSocialId(), userProvider.getUser().getRole().getRoleId(), null);
    }

    private Map<String, Object> profile(Map<String, Object> attributes) {
        Object response = attributes.get("response");
        if (!(response instanceof Map<?, ?> rawProfile)) {
            throw new BaseException(OAUTH2_AUTH_FAILED);
        }
        return rawProfile.entrySet().stream()
                .filter(entry -> entry.getKey() instanceof String)
                .collect(java.util.stream.Collectors.toMap(
                        entry -> (String) entry.getKey(),
                        Map.Entry::getValue
                ));
    }

    private String requiredAttribute(Map<String, Object> attributes, String name) {
        String value = optionalAttribute(attributes, name, null);
        if (value == null) {
            throw new BaseException(OAUTH2_AUTH_FAILED);
        }
        return value;
    }

    private String optionalAttribute(Map<String, Object> attributes, String name, String defaultValue) {
        Object value = attributes.get(name);
        if (!(value instanceof String text) || text.isBlank()) {
            return defaultValue;
        }
        return text;
    }
}
