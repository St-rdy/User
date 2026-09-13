package com.stardy.user.config;

import com.stardy.user.service.AuthService;
import com.stardy.user.service.GoogleOAuth2Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class OAuth2AuthenticationSuccessHandlerTest {

    @Test
    @DisplayName("Google 로그인 성공 시 JWT 대신 UUID 임시 코드를 프론트로 리다이렉트한다.")
    void redirectWithTemporaryCode() throws Exception {
        GoogleOAuth2Service googleOAuth2Service = mock(GoogleOAuth2Service.class);
        AuthService authService = mock(AuthService.class);
        OAuth2AuthenticationSuccessHandler handler = new OAuth2AuthenticationSuccessHandler(
                googleOAuth2Service,
                authService,
                "http://localhost:3000/oauth/callback"
        );
        DefaultOAuth2User oauthUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("sub", "google-sub", "email", "test@gmail.com"),
                "sub"
        );
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oauthUser,
                oauthUser.getAuthorities(),
                "google"
        );
        given(googleOAuth2Service.login(oauthUser.getAttributes()))
                .willReturn(new GoogleOAuth2Service.OAuthLoginUser("test@gmail.com", "ROLE_USER"));
        given(authService.issueTemporaryCode("test@gmail.com", "ROLE_USER"))
                .willReturn("550e8400-e29b-41d4-a716-446655440000");

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:3000/oauth/callback?code=550e8400-e29b-41d4-a716-446655440000");
    }
}
