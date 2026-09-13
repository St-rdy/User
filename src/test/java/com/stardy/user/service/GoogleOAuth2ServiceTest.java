package com.stardy.user.service;

import com.stardy.user.entity.Role;
import com.stardy.user.entity.User;
import com.stardy.user.entity.UserProvider;
import com.stardy.user.repository.RoleRepository;
import com.stardy.user.repository.UserProviderRepository;
import com.stardy.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class GoogleOAuth2ServiceTest {

    @InjectMocks
    private GoogleOAuth2Service googleOAuth2Service;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProviderRepository userProviderRepository;

    @Mock
    private RoleRepository roleRepository;

    @Test
    @DisplayName("기존 Google 소셜 계정이면 기존 사용자 정보를 반환한다.")
    void loginExistingGoogleUser() {
        User user = createUser("test@gmail.com", "ROLE_USER");
        UserProvider provider = new UserProvider(user, "GOOGLE", "google-sub", "test@gmail.com");
        given(userProviderRepository.findByProviderAndSocialId("GOOGLE", "google-sub"))
                .willReturn(Optional.of(provider));

        GoogleOAuth2Service.OAuthLoginUser result = googleOAuth2Service.login(Map.of("sub", "google-sub"));

        assertThat(result.email()).isEqualTo("test@gmail.com");
        assertThat(result.role()).isEqualTo("ROLE_USER");
        then(userRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("최초 Google 로그인 시 사용자, 기본 역할, 소셜 연동 정보를 생성한다.")
    void registerNewGoogleUser() {
        Role role = new Role("ROLE_USER", "일반 사용자");
        given(userProviderRepository.findByProviderAndSocialId("GOOGLE", "google-sub"))
                .willReturn(Optional.empty());
        given(userRepository.findByEmail("new@gmail.com")).willReturn(Optional.empty());
        given(roleRepository.findByRoleId("ROLE_USER")).willReturn(Optional.of(role));
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        GoogleOAuth2Service.OAuthLoginUser result = googleOAuth2Service.login(Map.of(
                "sub", "google-sub",
                "email", "new@gmail.com",
                "name", "New User",
                "picture", "https://example.com/profile.png"
        ));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        then(userRepository).should().save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("new@gmail.com");
        assertThat(savedUser.getNickname()).startsWith("google-");
        assertThat(savedUser.getDomain()).isEqualTo(Map.of("regions", List.of(), "subjects", List.of()));
        assertThat(savedUser.getRole()).isSameAs(role);
        then(userProviderRepository).should().save(any(UserProvider.class));
        assertThat(result).isEqualTo(new GoogleOAuth2Service.OAuthLoginUser("new@gmail.com", "ROLE_USER"));
    }

    private User createUser(String email, String roleId) {
        return new User(
                email,
                "Test User",
                "test-nickname",
                null,
                Map.of("regions", List.of(), "subjects", List.of()),
                new Role(roleId, "사용자"),
                "ACTIVE"
        );
    }
}
