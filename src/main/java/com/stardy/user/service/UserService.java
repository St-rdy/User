package com.stardy.user.service;

import com.stardy.user.dto.UserDto;
import com.stardy.user.entity.User;
import com.stardy.user.entity.UserProvider;
import com.stardy.user.exception.BaseException;
import com.stardy.user.global.BadWordFilter;
import com.stardy.user.global.DomainValidator;
import com.stardy.user.repository.UserProviderRepository;
import com.stardy.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.stardy.user.exception.ErrorCode.DUPLICATED_PROVIDER;
import static com.stardy.user.exception.ErrorCode.INVALID_PROVIDER;
import static com.stardy.user.exception.ErrorCode.INVALID_NICKNAME;
import static com.stardy.user.exception.ErrorCode.INVALID_PROFILE_IMAGE;
import static com.stardy.user.exception.ErrorCode.LAST_PROVIDER;
import static com.stardy.user.exception.ErrorCode.PROVIDER_NOT_FOUND;
import static com.stardy.user.exception.ErrorCode.UNAUTHORIZED;
import static com.stardy.user.exception.ErrorCode.USER_ALREADY_INACTIVE;
import static com.stardy.user.exception.ErrorCode.USER_NOT_FOUND;

@Service
public class UserService {

    private static final String CONSONANT_ONLY_REGEX = "^[ㄱ-ㅎ]+$";
    private static final String VOWEL_ONLY_REGEX = "^[ㅏ-ㅣ]+$";
    private static final Set<String> ALLOWED_PROVIDERS = Set.of("GOOGLE", "KAKAO", "NAVER");

    // Object Storage 설정 전 임시 코드
    private static final List<String> PROFILE_IMAGE_LIST = List.of(
            "https://example.com/profile-1.png",
            "https://example.com/profile-2.png",
            "https://example.com/profile-3.png"
    );

    private final UserRepository userRepository;
    private final UserProviderRepository userProviderRepository;
    private final BadWordFilter badWordFilter;
    private final DomainValidator domainValidator;

    public UserService(UserRepository userRepository, UserProviderRepository userProviderRepository, BadWordFilter badWordFilter, DomainValidator domainValidator) {
        this.userRepository = userRepository;
        this.userProviderRepository = userProviderRepository;
        this.badWordFilter = badWordFilter;
        this.domainValidator = domainValidator;
    }

    @Transactional(readOnly = true)
    public UserDto getUserInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(USER_NOT_FOUND));

        return UserDto.from(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUser(List<Long> userIds) {
        return userRepository.findByIdIn(userIds).stream()
                .map(UserDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public void checkNickname(String nickname){
        validateNickname(nickname);

        if (userRepository.findByNickname(nickname).isPresent()) {
            throw new BaseException(INVALID_NICKNAME);
        }
    }

    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(String nickname) {
        validateNickname(nickname);

        return userRepository.findByNickname(nickname).isEmpty();
    }

    private void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new BaseException(INVALID_NICKNAME);
        }

        if (nickname.matches(CONSONANT_ONLY_REGEX) || nickname.matches(VOWEL_ONLY_REGEX)) {
            throw new BaseException(INVALID_NICKNAME);
        }

        if (badWordFilter.containsBadWord(nickname)) {
            throw new BaseException(INVALID_NICKNAME);
        }
    }

    @Transactional
    public void changeNickname(String email, String nickname){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(USER_NOT_FOUND));

        checkNickname(nickname);
        user.changeNickname(nickname);
    }

    @Transactional
    public void changeDomain(String email, Map<String, Object> domain) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(USER_NOT_FOUND));

        validateDomain(domain);
        user.changeDomain(domain);
    }

    void validateDomain(Map<String, Object> domain) {
        domainValidator.validate(domain);
    }

    @Transactional
    public void changeProfileImage(String email, String profileImageUrl) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(USER_NOT_FOUND));

        validateProfileImage(profileImageUrl);

        user.changeProfileImage(profileImageUrl);
    }

    void validateProfileImage(String profileImageUrl) {
        if (profileImageUrl == null || !profileImageUrl.startsWith("https://")) {
            throw new BaseException(INVALID_PROFILE_IMAGE);
        }
    }

    @Transactional
    public void connectSocialLogin(String email, String provider, String socialId, String providerEmail) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(USER_NOT_FOUND));

        if (!ALLOWED_PROVIDERS.contains(provider)) {
            throw new BaseException(INVALID_PROVIDER);
        }

        if (userProviderRepository.existsByProviderAndSocialId(provider, socialId)) {
            throw new BaseException(DUPLICATED_PROVIDER);
        }

        UserProvider userProvider = new UserProvider(user, provider, socialId, providerEmail);
        userProviderRepository.save(userProvider);
    }

    @Transactional(readOnly = true)
    public List<String> getProfileImageList() {
        return PROFILE_IMAGE_LIST;
    }

    @Transactional(readOnly = true)
    public Map<String, List<Map<String, String>>> getDomainOptions() {
        return domainValidator.getDomainOptions();
    }

    @Transactional
    public void deleteSocialLogin(String email, String provider) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(USER_NOT_FOUND));

        if (!ALLOWED_PROVIDERS.contains(provider)) {
            throw new BaseException(INVALID_PROVIDER);
        }

        UserProvider userProvider = userProviderRepository.findByUserAndProvider(user, provider)
                .orElseThrow(() -> new BaseException(PROVIDER_NOT_FOUND));

        if (userProviderRepository.countByUser(user) <= 1) {
            throw new BaseException(LAST_PROVIDER);
        }

        userProviderRepository.delete(userProvider);
    }

    @Transactional
    public void deleteUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(USER_NOT_FOUND));

        if ("INACTIVE".equals(user.getStatus())) {
            throw new BaseException(USER_ALREADY_INACTIVE);
        }

        user.deleteUser();
    }

    @Transactional
    public void bannedUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(USER_NOT_FOUND));

        user.bannedUser();
    }

    @Transactional
    public void changeUserStatusByAdmin(String adminEmail, String targetEmail, String status) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BaseException(USER_NOT_FOUND));
        User targetUser = userRepository.findByEmail(targetEmail)
                .orElseThrow(() -> new BaseException(USER_NOT_FOUND));

        if (!"ROLE_ADMIN".equals(admin.getRole().getRoleId())) {
            throw new BaseException(UNAUTHORIZED);
        }

        targetUser.changeStatus(status);
    }
}
