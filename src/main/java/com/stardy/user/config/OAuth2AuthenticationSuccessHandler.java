package com.stardy.user.config;

import com.stardy.user.service.AuthService;
import com.stardy.user.service.GoogleOAuth2Service;
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

    private final GoogleOAuth2Service googleOAuth2Service;
    private final AuthService authService;
    private final String frontendRedirectUri;

    public OAuth2AuthenticationSuccessHandler(
            GoogleOAuth2Service googleOAuth2Service,
            AuthService authService,
            @Value("${app.oauth2.frontend-redirect-uri}") String frontendRedirectUri
    ) {
        this.googleOAuth2Service = googleOAuth2Service;
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
        if (!GOOGLE.equals(oauthToken.getAuthorizedClientRegistrationId())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        OAuth2User oauthUser = oauthToken.getPrincipal();
        GoogleOAuth2Service.OAuthLoginUser user = googleOAuth2Service.login(oauthUser.getAttributes());
        String temporaryCode = authService.issueTemporaryCode(user.email(), user.role());

        String redirectUri = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("code", temporaryCode)
                .build()
                .toUriString();

        response.sendRedirect(redirectUri);
    }
}
