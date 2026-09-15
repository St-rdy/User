package com.stardy.user.service;

import com.stardy.user.repository.UserProviderRepository;
import com.stardy.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class NaverOAuth2ServiceTest {

    @InjectMocks
    private NaverOAuth2Service naverOAuth2Service;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProviderRepository userProviderRepository;

    @Test
    @DisplayName("최초 Naver 로그인은 DB에 저장하지 않고 가입대기 정보를 반환한다.")
    void prepareSignupForNewNaverUser() {
        given(userProviderRepository.findByProviderAndSocialId("NAVER", "naver-id")).willReturn(Optional.empty());

        GoogleOAuth2Service.OAuthLoginUser result = naverOAuth2Service.login(Map.of(
                "response", Map.of("id", "naver-id", "email", "new@naver.com", "name", "Naver User")
        ));

        assertThat(result.requiresSignup()).isTrue();
        assertThat(result.signupInfo().provider()).isEqualTo("NAVER");
        assertThat(result.signupInfo().socialId()).isEqualTo("naver-id");
        then(userRepository).shouldHaveNoInteractions();
        then(userProviderRepository).shouldHaveNoMoreInteractions();
    }
}
