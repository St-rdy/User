package com.stardy.user.service;

import com.stardy.user.dto.OAuthSignupInfoDto;
import com.stardy.user.entity.Role;
import com.stardy.user.entity.User;
import com.stardy.user.entity.UserProvider;
import com.stardy.user.exception.BaseException;
import com.stardy.user.repository.RoleRepository;
import com.stardy.user.repository.UserProviderRepository;
import com.stardy.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static com.stardy.user.exception.ErrorCode.OAUTH2_AUTH_FAILED;

@Service
public class GoogleOAuth2Service {

    private static final String GOOGLE = "GOOGLE";
    private static final String DEFAULT_ROLE_ID = "ROLE_USER";

    private final UserRepository userRepository;
    private final UserProviderRepository userProviderRepository;
    private final RoleRepository roleRepository;
    private final UserService userService;

    public GoogleOAuth2Service(
            UserRepository userRepository,
            UserProviderRepository userProviderRepository,
            RoleRepository roleRepository,
            UserService userService
    ) {
        this.userRepository = userRepository;
        this.userProviderRepository = userProviderRepository;
        this.roleRepository = roleRepository;
        this.userService = userService;
    }

    @Transactional
    public OAuthLoginUser login(Map<String, Object> attributes) {
        String socialId = requiredAttribute(attributes, "sub");

        return userProviderRepository.findByProviderAndSocialId(GOOGLE, socialId)
                .map(userProvider -> OAuthLoginUser.existing(
                        userProvider.getProvider(), userProvider.getSocialId(), userProvider.getUser()))
                .orElseGet(() -> prepareSignup(attributes, socialId));
    }

    private OAuthLoginUser prepareSignup(Map<String, Object> attributes, String socialId) {
        String email = requiredAttribute(attributes, "email");
        String name = optionalAttribute(attributes, "name", email);

        return OAuthLoginUser.signup(new OAuthSignupInfoDto(email, name, DEFAULT_ROLE_ID, GOOGLE, socialId, email));
    }

    @Transactional
    public OAuthLoginUser completeSignup(
            OAuthSignupInfoDto signupInfo,
            String nickname,
            String profileImageUrl,
            Map<String, Object> domain
    ) {
        userService.checkNickname(nickname);
        userService.validateProfileImage(profileImageUrl);
        userService.validateDomain(domain);

        if (userProviderRepository.existsByProviderAndSocialId(signupInfo.provider(), signupInfo.socialId())) {
            throw new BaseException(OAUTH2_AUTH_FAILED);
        }

        User user = userRepository.save(new User(
                signupInfo.email(),
                signupInfo.name(),
                nickname,
                profileImageUrl,
                domain,
                getRole(signupInfo.role()),
                "ACTIVE"
        ));
        userProviderRepository.save(new UserProvider(user, signupInfo.provider(), signupInfo.socialId(), signupInfo.providerEmail()));

        return OAuthLoginUser.existing(signupInfo.provider(), signupInfo.socialId(), user);
    }

    private Role getRole(String roleId) {
        return roleRepository.findByRoleId(roleId)
                .orElseGet(() -> roleRepository.save(new Role(roleId, "일반 사용자")));
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

    public record OAuthLoginUser(String provider, String socialId, String role, OAuthSignupInfoDto signupInfo) {
        @Deprecated
        public OAuthLoginUser(String socialId, String role, OAuthSignupInfoDto signupInfo) {
            this(signupInfo == null ? "LEGACY" : signupInfo.provider(), socialId, role, signupInfo);
        }

        @Deprecated
        public String email() {
            return socialId;
        }
        public static OAuthLoginUser signup(OAuthSignupInfoDto signupInfo) {
            return new OAuthLoginUser(signupInfo.provider(), signupInfo.socialId(), signupInfo.role(), signupInfo);
        }

        public boolean requiresSignup() {
            return signupInfo != null;
        }

        private static OAuthLoginUser existing(String provider, String socialId, User user) {
            if (user.getRole() == null) {
                throw new BaseException(OAUTH2_AUTH_FAILED);
            }
            return new OAuthLoginUser(provider, socialId, user.getRole().getRoleId(), null);
        }
    }
}
