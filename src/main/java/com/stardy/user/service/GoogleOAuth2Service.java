package com.stardy.user.service;

import com.stardy.user.entity.Role;
import com.stardy.user.entity.User;
import com.stardy.user.entity.UserProvider;
import com.stardy.user.exception.BaseException;
import com.stardy.user.repository.RoleRepository;
import com.stardy.user.repository.UserProviderRepository;
import com.stardy.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.stardy.user.exception.ErrorCode.OAUTH2_AUTH_FAILED;

@Service
public class GoogleOAuth2Service {

    private static final String GOOGLE = "GOOGLE";
    private static final String DEFAULT_ROLE_ID = "ROLE_USER";

    private final UserRepository userRepository;
    private final UserProviderRepository userProviderRepository;
    private final RoleRepository roleRepository;

    public GoogleOAuth2Service(
            UserRepository userRepository,
            UserProviderRepository userProviderRepository,
            RoleRepository roleRepository
    ) {
        this.userRepository = userRepository;
        this.userProviderRepository = userProviderRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public OAuthLoginUser login(Map<String, Object> attributes) {
        String socialId = requiredAttribute(attributes, "sub");

        return userProviderRepository.findByProviderAndSocialId(GOOGLE, socialId)
                .map(userProvider -> OAuthLoginUser.from(userProvider.getUser()))
                .orElseGet(() -> register(attributes, socialId));
    }

    private OAuthLoginUser register(Map<String, Object> attributes, String socialId) {
        String email = requiredAttribute(attributes, "email");
        String name = optionalAttribute(attributes, "name", email);
        String profileImageUrl = optionalAttribute(attributes, "picture", null);

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(new User(
                        email,
                        name,
                        createNickname(),
                        profileImageUrl,
                        Map.of("regions", List.of(), "subjects", List.of()),
                        getDefaultRole(),
                        "ACTIVE"
                )));

        userProviderRepository.save(new UserProvider(user, GOOGLE, socialId, email));

        return OAuthLoginUser.from(user);
    }

    private Role getDefaultRole() {
        return roleRepository.findByRoleId(DEFAULT_ROLE_ID)
                .orElseGet(() -> roleRepository.save(new Role(DEFAULT_ROLE_ID, "일반 사용자")));
    }

    private String createNickname() {
        return "google-" + UUID.randomUUID().toString().substring(0, 8);
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

    public record OAuthLoginUser(String email, String role) {
        private static OAuthLoginUser from(User user) {
            if (user.getRole() == null) {
                throw new BaseException(OAUTH2_AUTH_FAILED);
            }
            return new OAuthLoginUser(user.getEmail(), user.getRole().getRoleId());
        }
    }
}
