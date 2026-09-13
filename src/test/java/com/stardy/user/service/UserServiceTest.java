package com.stardy.user.service;

import com.stardy.user.dto.UserDto;
import com.stardy.user.entity.Role;
import com.stardy.user.entity.User;
import com.stardy.user.entity.UserProvider;
import com.stardy.user.exception.BaseException;
import com.stardy.user.exception.ErrorCode;
import com.stardy.user.global.BadWordFilter;
import com.stardy.user.global.DomainValidator;
import com.stardy.user.repository.UserProviderRepository;
import com.stardy.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProviderRepository userProviderRepository;

    @Mock
    private BadWordFilter badWordFilter;

    @Mock
    private DomainValidator domainValidator;

    @Test
    @DisplayName("유저의 모든 정보를 조회한다. (내정보 조회 기능)")
    void getUserInfo() {
        String email = "test@gmail.com";
        String role = "ROLE_USER";
        Map<String, Object> domain = Map.of(
                "regions", List.of("Seoul"),
                "subjects", List.of("Math")
        );

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.of(createUserWithRole(email, role)));

        // When
        UserDto result = userService.getUserInfo(email);

        // Then
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getName()).isEqualTo("Test User");
        assertThat(result.getNickname()).isEqualTo("test-nickname");
        assertThat(result.getProfileImageUrl()).isEqualTo("https://example.com/profile.png");
        assertThat(result.getDomain()).isEqualTo(domain);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getRoleId()).isEqualTo("ROLE_USER");
    }

    // 확인 필요
    @Test
    @DisplayName("여러 유저의 모든 정보를 조회한다. (List 타입으로 반환)")
    void getAllUser() {
        List<Long> userIds = List.of(1L, 2L, 3L);
        List<User> users = List.of(
                createUserWithRole("test1@gmail.com", "ROLE_USER"),
                createUserWithRole("test2@gmail.com", "ROLE_USER"),
                createUserWithRole("test3@gmail.com", "ROLE_ADMIN")
        );

        // Given
        given(userRepository.findByIdIn(userIds)).willReturn(users);

        // When
        List<UserDto> result = userService.getAllUser(userIds);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result)
                .extracting(UserDto::getNickname)
                .containsExactly("test-nickname", "test-nickname", "test-nickname");
        assertThat(result)
                .extracting(UserDto::getEmail)
                .containsExactly("test1@gmail.com", "test2@gmail.com", "test3@gmail.com");
        assertThat(result)
                .extracting(UserDto::getProfileImageUrl)
                .containsExactly(
                        "https://example.com/profile.png",
                        "https://example.com/profile.png",
                        "https://example.com/profile.png"
                );
        assertThat(result)
                .extracting(UserDto::getDomain)
                .containsExactly(
                        Map.of("regions", List.of("Seoul"), "subjects", List.of("Math")),
                        Map.of("regions", List.of("Seoul"), "subjects", List.of("Math")),
                        Map.of("regions", List.of("Seoul"), "subjects", List.of("Math"))
                );
    }

    @Test
    @DisplayName("조회할 유저 ID가 없으면 빈 목록을 반환한다.")
    void getAllUserWithEmptyIds() {
        List<Long> emptyIds = List.of();

        // Given
        given(userRepository.findByIdIn(emptyIds)).willReturn(List.of());

        // When
        List<UserDto> result = userService.getAllUser(emptyIds);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("유저의 정보가 없다면 유저가 정보를 확인할 수 없다.")
    void getUserInfoWithNoUser() {
        String email = "notfound@gmail.com";

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // When
        BaseException exception = assertThrows(BaseException.class, () -> userService.getUserInfo(email));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임은 사용할 수 없다.")
    void checkNicknameWithDuplicatedNickname() {
        String nickname = "test-nickname";
        User mockUser = mock(User.class);

        // Given
        given(badWordFilter.containsBadWord(nickname)).willReturn(false);
        given(userRepository.findByNickname(nickname)).willReturn(Optional.of(mockUser));

        // When
        BaseException exception = assertThrows(BaseException.class, () -> userService.checkNickname(nickname));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_NICKNAME);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.INVALID_NICKNAME.getMessage());
    }

    @Test
    @DisplayName("유효하지 않은 형식의 닉네임은 사용할 수 없다.")
    void checkNicknameWithInvalidFormat() {
        String consonantOnlyNickname = "ㄱㄴㄷ";
        String vowelOnlyNickname = "ㅕㅛㅣ";

        BaseException consonantException = assertThrows(BaseException.class, () -> userService.checkNickname(consonantOnlyNickname));
        BaseException vowelException = assertThrows(BaseException.class, () -> userService.checkNickname(vowelOnlyNickname));

        assertThat(consonantException.getErrorCode()).isEqualTo(ErrorCode.INVALID_NICKNAME);
        assertThat(consonantException.getMessage()).isEqualTo(ErrorCode.INVALID_NICKNAME.getMessage());
        assertThat(vowelException.getErrorCode()).isEqualTo(ErrorCode.INVALID_NICKNAME);
        assertThat(vowelException.getMessage()).isEqualTo(ErrorCode.INVALID_NICKNAME.getMessage());
    }

    @Test
    @DisplayName("유저는 욕설과 관련된 단어로 닉네임을 설정할 수 없다.")
    void changeNicknameWithInvalidNickname() {
        String InvalidNickname = "쌰갈";

        given(badWordFilter.containsBadWord(InvalidNickname)).willReturn(true);

        BaseException consonantException = assertThrows(BaseException.class, () -> userService.checkNickname(InvalidNickname));

        assertThat(consonantException.getErrorCode()).isEqualTo(ErrorCode.INVALID_NICKNAME);
        assertThat(consonantException.getMessage()).isEqualTo(ErrorCode.INVALID_NICKNAME.getMessage());
    }

    @Test
    @DisplayName("유저의 정보가 없다면 유저는 닉네임을 변경할 수 없다.")
    void changeNicknameWithNoUser() {
        String email = "notfound@gmail.com";
        String newNickname = "new-nickname";

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // When
        BaseException exception = assertThrows(BaseException.class, () -> userService.changeNickname(email, newNickname));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("유저는 빈 닉네임으로 변경할 수 없다.")
    void changeNicknameWithBlankNickname() {
        String EmptyNickname = "";

        // Given
        BaseException consonantException = assertThrows(BaseException.class, () -> userService.checkNickname(EmptyNickname));

        // Then
        assertThat(consonantException.getErrorCode()).isEqualTo(ErrorCode.INVALID_NICKNAME);
        assertThat(consonantException.getMessage()).isEqualTo(ErrorCode.INVALID_NICKNAME.getMessage());
    }

    @Test
    @DisplayName("공백으로만 이루어진 닉네임은 사용할 수 없다.")
    void checkNicknameWithWhitespaceNickname() {
        String whitespaceNickname = "   ";

        // When
        BaseException exception = assertThrows(BaseException.class, () -> userService.checkNickname(whitespaceNickname));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_NICKNAME);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.INVALID_NICKNAME.getMessage());
    }

    @Test
    @DisplayName("유저가 닉네임을 변경한다.")
    void changeNickname() {
        String email = "test@gmal.com";
        String role = "ROLE_USER";
        String newNickname = "NewNickname";
        User user = createUserWithRole(email, role);

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));                   // 유저의 이메일이 존재한다.
        given(badWordFilter.containsBadWord(newNickname)).willReturn(false);                 // 비속어가 포함되어 있지 않다.
        given(userRepository.findByNickname(newNickname)).willReturn(Optional.empty());     // 중복 닉네임이 없다.

        // When
        userService.changeNickname(email, newNickname);

        // Then
        assertThat(user.getNickname()).isEqualTo(newNickname);
    }

    @Test
    @DisplayName("유저가 도메인(지역, 과목)을 변경한다.")
    void changeDomain() {
        String email = "test@gmail.com";
        String role = "ROLE_USER";
        User user = createUserWithRole(email, role);
        Map<String, Object> newDomain = Map.of(
                "regions", List.of("Busan"),
                "subjects", List.of("English")
        );

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // When
        userService.changeDomain(email, newDomain);

        // Then
        assertThat(user.getDomain()).isEqualTo(newDomain);
    }

    @Test
    @DisplayName("유저의 정보가 없다면 도메인을 변경할 수 없다.")
    void changeDomainWithNoUser() {
        String email = "notfound@gmail.com";
        Map<String, Object> newDomain = Map.of(
                "regions", List.of("Busan"),
                "subjects", List.of("English")
        );

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // When
        BaseException exception = assertThrows(BaseException.class, () -> userService.changeDomain(email, newDomain));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("유효하지 않은 도메인 형식으로 변경할 수 없다.")
    void changeDomainWithInvalidDomain() {
        String email = "test@gmail.com";
        String role = "ROLE_USER";
        User user = createUserWithRole(email, role);
        Map<String, Object> invalidDomain = Map.of(
                "regions", List.of("American")
        );

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        willThrow(new BaseException(ErrorCode.INVALID_DOMAIN))
                .given(domainValidator)
                .validate(invalidDomain);

        // When
        BaseException exception = assertThrows(BaseException.class, () -> userService.changeDomain(email, invalidDomain));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_DOMAIN);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.INVALID_DOMAIN.getMessage());
    }

    @Test
    @DisplayName("빈 도메인으로 변경할 수 없다.")
    void changeDomainWithEmptyDomain() {
        String email = "test@gmail.com";
        String role = "ROLE_USER";
        User user = createUserWithRole(email, role);
        Map<String, Object> emptyDomain = Map.of();

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        willThrow(new BaseException(ErrorCode.INVALID_DOMAIN))
                .given(domainValidator)
                .validate(emptyDomain);

        // When
        BaseException exception = assertThrows(BaseException.class, () -> userService.changeDomain(email, emptyDomain));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_DOMAIN);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.INVALID_DOMAIN.getMessage());
    }

    @Test
    @DisplayName("유저가 프로필 이미지를 변경한다.")
    void changeProfileImage() {
        String email = "test@gmail.com";
        String role = "ROLE_USER";
        String newProfileImageUrl = "https://example.com/new-profile.png";
        User user = createUserWithRole(email, role);

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // When
        userService.changeProfileImage(email, newProfileImageUrl);

        // Then
        assertThat(user.getProfileImageUrl()).isEqualTo(newProfileImageUrl);
    }

    @Test
    @DisplayName("유저의 정보가 없다면 프로필 이미지를 변경할 수 없다.")
    void changeProfileImageWithNoUser() {
        String email = "notfound@gmail.com";
        String newProfileImageUrl = "https://example.com/new-profile.png";

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // When
        BaseException exception = assertThrows(BaseException.class, () -> userService.changeProfileImage(email, newProfileImageUrl));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("프로필 이미지가 없는 URL로 변경하려고 할 때 변경할 수 없다.")
    void changeProfileImageWithInvalidUrl() {
        String email = "test@gmail.com";
        String role = "ROLE_USER";
        String invalidProfileImageUrl = "not-url";
        User user = createUserWithRole(email, role);

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // When
        BaseException exception = assertThrows(BaseException.class, () -> userService.changeProfileImage(email, invalidProfileImageUrl));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_PROFILE_IMAGE);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.INVALID_PROFILE_IMAGE.getMessage());
    }

    @Test
    @DisplayName("null URL로 프로필 이미지를 변경할 수 없다.")
    void changeProfileImageWithNullUrl() {
        String email = "test@gmail.com";
        String role = "ROLE_USER";
        User user = createUserWithRole(email, role);

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // When
        BaseException exception = assertThrows(BaseException.class, () -> userService.changeProfileImage(email, null));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_PROFILE_IMAGE);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.INVALID_PROFILE_IMAGE.getMessage());
    }

    @Test
    @DisplayName("유저가 선택 가능한 프로필 이미지 목록을 조회한다.")
    void getProfileImageList() {
        // When
        List<String> result = userService.getProfileImageList();

        // Then
        assertThat(result).containsExactly(
                "https://example.com/profile-1.png",
                "https://example.com/profile-2.png",
                "https://example.com/profile-3.png"
        );
    }

    @Test
    @DisplayName("유저가 다른 소셜 로그인 정보를 연동한다.")
    void connectSocialLogin() {
        String email = "test@gmail.com";
        String role = "ROLE_USER";
        String provider = "GOOGLE";
        String socialId = "google-social-id";
        String providerEmail = "google@gmail.com";
        User user = createUserWithRole(email, role);

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // When
        userService.connectSocialLogin(email, provider, socialId, providerEmail);

        // Then
        ArgumentCaptor<UserProvider> captor = ArgumentCaptor.forClass(UserProvider.class);
        then(userProviderRepository).should().save(captor.capture());
        UserProvider savedProvider = captor.getValue();

        assertThat(savedProvider.getUser()).isEqualTo(user);
        assertThat(savedProvider.getProvider()).isEqualTo(provider);
        assertThat(savedProvider.getSocialId()).isEqualTo(socialId);
        assertThat(savedProvider.getProviderEmail()).isEqualTo(providerEmail);
        assertThat(savedProvider.getConnectedAt()).isNotNull();
    }

    @Test
    @DisplayName("유저의 정보가 없다면 소셜 로그인 정보를 연동할 수 없다.")
    void connectSocialLoginWithNoUser() {
        String email = "notfound@gmail.com";
        String provider = "GOOGLE";
        String socialId = "google-social-id";
        String providerEmail = "google@gmail.com";

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // When
        BaseException exception = assertThrows(BaseException.class, () -> userService.connectSocialLogin(email, provider, socialId, providerEmail));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("지원하지 않는 소셜 로그인 정보는 연동할 수 없다.")
    void connectSocialLoginWithInvalidProvider() {
        String email = "test@gmail.com";
        String role = "ROLE_USER";
        String provider = "FACEBOOK";
        String socialId = "facebook-social-id";
        String providerEmail = "facebook@gmail.com";
        User user = createUserWithRole(email, role);

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // When
        BaseException exception = assertThrows(BaseException.class, () -> userService.connectSocialLogin(email, provider, socialId, providerEmail));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_PROVIDER);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.INVALID_PROVIDER.getMessage());
    }

    @Test
    @DisplayName("이미 다른 계정에 연결된 소셜 로그인 정보는 연동할 수 없다.")
    void connectSocialLoginWithDuplicatedProviderSocialId() {
        String email = "test@gmail.com";
        String role = "ROLE_USER";
        String provider = "GOOGLE";
        String socialId = "google-social-id";
        String providerEmail = "google@gmail.com";
        User user = createUserWithRole(email, role);

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(userProviderRepository.existsByProviderAndSocialId(provider, socialId)).willReturn(true);

        // When
        BaseException exception = assertThrows(BaseException.class, () -> userService.connectSocialLogin(email, provider, socialId, providerEmail));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATED_PROVIDER);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.DUPLICATED_PROVIDER.getMessage());
    }

    @Test
    @DisplayName("연동되어 있는 소셜로그인 정보를 해제한다.")
    void deleteSocialLogin() {
        String email = "test@gmail.com";
        String role = "ROLE_USER";
        String provider = "GOOGLE";
        User user = createUserWithRole(email, role);
        UserProvider userProvider = new UserProvider(user, provider, "google-social-id", "google@gmail.com");

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(userProviderRepository.findByUserAndProvider(user, provider)).willReturn(Optional.of(userProvider));
        given(userProviderRepository.countByUser(user)).willReturn(2L);

        // When
        userService.deleteSocialLogin(email, provider);

        // Then
        then(userProviderRepository).should().delete(userProvider);
    }

    @Test
    @DisplayName("유저의 정보가 없다면 소셜 로그인 정보를 해제할 수 없다.")
    void deleteSocialLoginWithNoUser() {
        String email = "notfound@gmail.com";
        String provider = "GOOGLE";

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // When
        BaseException exception = assertThrows(BaseException.class, () -> userService.deleteSocialLogin(email, provider));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("지원하지 않는 소셜 로그인 정보는 해제할 수 없다.")
    void deleteSocialLoginWithInvalidProvider() {
        String email = "test@gmail.com";
        String role = "ROLE_USER";
        String provider = "FACEBOOK";
        User user = createUserWithRole(email, role);

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // When
        BaseException exception = assertThrows(BaseException.class, () -> userService.deleteSocialLogin(email, provider));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_PROVIDER);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.INVALID_PROVIDER.getMessage());
    }

    @Test
    @DisplayName("연동되어 있지 않은 소셜 로그인 정보는 해제할 수 없다.")
    void deleteSocialLoginWithProviderNotFound() {
        String email = "test@gmail.com";
        String role = "ROLE_USER";
        String provider = "GOOGLE";
        User user = createUserWithRole(email, role);

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(userProviderRepository.findByUserAndProvider(user, provider)).willReturn(Optional.empty());

        // When
        BaseException exception = assertThrows(BaseException.class, () -> userService.deleteSocialLogin(email, provider));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROVIDER_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.PROVIDER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("소셜로그인이 하나일 경우 연동해제를 할 수 없다.")
    void deleteSocialLoginWithLastProvider() {
        String email = "test@gmail.com";
        String role = "ROLE_USER";
        String provider = "GOOGLE";
        User user = createUserWithRole(email, role);
        UserProvider userProvider = new UserProvider(user, provider, "google-social-id", "google@gmail.com");

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(userProviderRepository.findByUserAndProvider(user, provider)).willReturn(Optional.of(userProvider));
        given(userProviderRepository.countByUser(user)).willReturn(1L);

        // When
        BaseException exception = assertThrows(BaseException.class, () -> userService.deleteSocialLogin(email, provider));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.LAST_PROVIDER);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.LAST_PROVIDER.getMessage());
    }

    @Test
    @DisplayName("계정을 탈퇴한다.")
    void deleteUser() {
        String email = "test@gmail.com";
        String role = "ROLE_USER";
        User user = createUserWithRole(email, role);

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // When
        userService.deleteUser(email);

        // Then
        assertThat(user.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    @DisplayName("유저의 정보가 없다면 계정을 탈퇴할 수 없다.")
    void deleteUserWithNoUser() {
        String email = "notfound@gmail.com";

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // When
        BaseException exception = assertThrows(BaseException.class, () -> userService.deleteUser(email));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("이미 탈퇴한 계정은 다시 탈퇴할 수 없다.")
    void deleteUserAlreadyWithdrawal() {
        String email = "test@gmail.com";
        String role = "ROLE_USER";
        User user = createUserWithRole(email, role);
        user.deleteUser();

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // When
        BaseException exception = assertThrows(BaseException.class, () -> userService.deleteUser(email));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_ALREADY_INACTIVE);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.USER_ALREADY_INACTIVE.getMessage());
    }

    @Test
    @DisplayName("계정을 정지한다.")
    void bannedUser() {
        String email = "test@gmail.com";
        String role = "ROLE_USER";
        User user = createUserWithRole(email, role);

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // When
        userService.bannedUser(email);

        // Then
        assertThat(user.getStatus()).isEqualTo("BANNED");
    }

    @Test
    @DisplayName("유저의 정보가 없다면 계정을 정지할 수 없다.")
    void bannedUserWithNoUser() {
        String email = "notfound@gmail.com";

        // Given
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // When
        BaseException exception = assertThrows(BaseException.class, () -> userService.bannedUser(email));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("관리자 계정은 특정 유저의 상태를 변경할 수 있다.")
    void adminCanChangeUserStatus() {
        String adminEmail = "admin@gmail.com";
        String targetEmail = "target@gmail.com";
        User admin = createUserWithRole(adminEmail, "ROLE_ADMIN");
        User targetUser = createUserWithRole(targetEmail, "ROLE_USER");

        // Given
        given(userRepository.findByEmail(adminEmail)).willReturn(Optional.of(admin));
        given(userRepository.findByEmail(targetEmail)).willReturn(Optional.of(targetUser));

        // When
        userService.changeUserStatusByAdmin(adminEmail, targetEmail, "BANNED");
        assertThat(targetUser.getStatus()).isEqualTo("BANNED");

        userService.changeUserStatusByAdmin(adminEmail, targetEmail, "INACTIVE");
        assertThat(targetUser.getStatus()).isEqualTo("INACTIVE");

        userService.changeUserStatusByAdmin(adminEmail, targetEmail, "ACTIVE");
        assertThat(targetUser.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("일반 유저는 상태 변경을 할 수 없다.")
    void normalUserCanNotChangeUserStatus() {
        String normalUserEmail = "user@gmail.com";
        String targetEmail = "target@gmail.com";
        User user = createUserWithRole(normalUserEmail, "ROLE_USER");
        User targetUser = createUserWithRole(targetEmail, "ROLE_USER");

        given(userRepository.findByEmail(normalUserEmail)).willReturn(Optional.of(user));
        given(userRepository.findByEmail(targetEmail)).willReturn(Optional.of(targetUser));

        BaseException exception = assertThrows(BaseException.class, () -> userService.changeUserStatusByAdmin(normalUserEmail, targetEmail, "ACTIVE"));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.UNAUTHORIZED.getMessage());
        assertThat(targetUser.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("관리자 계정이 존재하지 않으면 유저 상태를 변경할 수 없다.")
    void changeUserStatusByAdminWithNoAdmin() {
        String adminEmail = "notfound@gmail.com";
        String targetEmail = "target@gmail.com";

        // Given
        given(userRepository.findByEmail(adminEmail)).willReturn(Optional.empty());

        // When
        BaseException exception = assertThrows(BaseException.class, () -> userService.changeUserStatusByAdmin(adminEmail, targetEmail, "BANNED"));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("대상 유저가 존재하지 않으면 상태를 변경할 수 없다.")
    void changeUserStatusByAdminWithNoTarget() {
        String adminEmail = "admin@gmail.com";
        String targetEmail = "notfound@gmail.com";
        User admin = createUserWithRole(adminEmail, "ROLE_ADMIN");

        // Given
        given(userRepository.findByEmail(adminEmail)).willReturn(Optional.of(admin));
        given(userRepository.findByEmail(targetEmail)).willReturn(Optional.empty());

        // When
        BaseException exception = assertThrows(BaseException.class, () -> userService.changeUserStatusByAdmin(adminEmail, targetEmail, "BANNED"));

        // Then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("도메인 JSON을 프론트에 전달한다.")
    void giveDomainNameToFront() {
        Map<String, List<Map<String, String>>> expected = Map.of(
                "regions", List.of(
                        Map.of("value", "Seoul", "label", "서울"),
                        Map.of("value", "Busan", "label", "부산")
                ),
                "subjects", List.of(
                        Map.of("value", "Korean", "label", "국어"),
                        Map.of("value", "Mathematics", "label", "수학")
                )
        );

        // Given
        given(domainValidator.getDomainOptions()).willReturn(expected);

        // When
        Map<String, List<Map<String, String>>> result = userService.getDomainOptions();

        // Then
        assertThat(result).isEqualTo(expected);
        assertThat(result.get("regions")).hasSize(2);
        assertThat(result.get("subjects")).hasSize(2);
    }

    private User createUserWithRole(String email, String roleName) {
        User user = new User(
                email,
                "Test User",
                "test-nickname",
                "https://example.com/profile.png",
                Map.of(
                    "regions", List.of("Seoul"),
                    "subjects", List.of("Math")
                ),
                new Role(roleName, roleDisplayName(roleName)),
                "ACTIVE"
        );
        return user;
    }

    private String roleDisplayName(String roleId) {
        if ("ROLE_ADMIN".equals(roleId)) {
            return "관리자";
        }
        return "일반 사용자";
    }
}
