package com.stardy.user.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
class OAuth2AuthorizationEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("API 버전 경로의 Google OAuth2 시작 요청은 Google 인증 페이지로 리다이렉트한다.")
    void redirectToGoogleAuthorizationPage() throws Exception {
        mockMvc.perform(get("/api/v1/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("accounts.google.com")));
    }

    @Test
    @DisplayName("API 버전 경로의 Naver OAuth2 시작 요청은 Naver 인증 페이지로 리다이렉트한다.")
    void redirectToNaverAuthorizationPage() throws Exception {
        mockMvc.perform(get("/api/v1/oauth2/authorization/naver"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("nid.naver.com/oauth2.0/authorize")));
    }
}
