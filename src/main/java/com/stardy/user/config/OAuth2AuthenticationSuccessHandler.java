package com.stardy.user.config;

import com.stardy.user.service.AuthService;
import com.stardy.user.service.GoogleOAuth2Service;
import com.stardy.user.service.NaverOAuth2Service;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final String GOOGLE = "google";
    private static final String NAVER = "naver";

    private final GoogleOAuth2Service googleOAuth2Service;
    private final NaverOAuth2Service naverOAuth2Service;
    private final AuthService authService;
    private final String frontendRedirectUri;

    public OAuth2AuthenticationSuccessHandler(
            GoogleOAuth2Service googleOAuth2Service,
            NaverOAuth2Service naverOAuth2Service,
            AuthService authService,
            @Value("${app.oauth2.frontend-redirect-uri}") String frontendRedirectUri
    ) {
        this.googleOAuth2Service = googleOAuth2Service;
        this.naverOAuth2Service = naverOAuth2Service;
        this.authService = authService;
        this.frontendRedirectUri = frontendRedirectUri;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        if (!GOOGLE.equals(registrationId) && !NAVER.equals(registrationId)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        OAuth2User oauthUser = oauthToken.getPrincipal();
        GoogleOAuth2Service.OAuthLoginUser user = GOOGLE.equals(registrationId)
                ? googleOAuth2Service.login(oauthUser.getAttributes())
                : naverOAuth2Service.login(oauthUser.getAttributes());
        String temporaryCode = user.requiresSignup()
                ? authService.issueTemporaryCode(user.provider(), user.socialId(), user.role(), user.signupInfo())
                : authService.issueTemporaryCode(user.provider(), user.socialId(), user.role());

        String redirectUri = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("code", temporaryCode)
                .queryParam("signupRequired", user.requiresSignup())
                .build()
                .toUriString();

        response.sendRedirect(redirectUri);
    }
}
