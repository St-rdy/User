package com.stardy.user.service;

import com.stardy.user.dto.OAuthSignupInfoDto;
import com.stardy.user.dto.TokenResponseDto;
import com.stardy.user.exception.BaseException;
import com.stardy.user.global.JwtProvider;
import com.stardy.user.repository.RedisTokenRepository;
import com.stardy.user.repository.UserProviderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static com.stardy.user.exception.ErrorCode.*;

@Service
public class AuthService {

    private final JwtProvider jwtProvider;
    private final RedisTokenRepository redisTokenRepository;
    private final UserProviderRepository userProviderRepository;
    private final GoogleOAuth2Service googleOAuth2Service;

    public AuthService(JwtProvider jwtProvider, RedisTokenRepository redisTokenRepository, UserProviderRepository userProviderRepository,
                       GoogleOAuth2Service googleOAuth2Service) {
        this.jwtProvider = jwtProvider;
        this.redisTokenRepository = redisTokenRepository;
        this.userProviderRepository = userProviderRepository;
        this.googleOAuth2Service = googleOAuth2Service;
    }

    @Transactional(readOnly = true)
    public TokenResponseDto reissueToken(String refreshToken){
        if(!jwtProvider.isTokenValid(refreshToken)){
            throw new BaseException(INVALID_TOKEN);
        }

        String socialId = jwtProvider.extractSocialId(refreshToken);
        String provider = jwtProvider.extractProvider(refreshToken);
        String identity = identity(provider, socialId);
        String role = userProviderRepository.findByProviderAndSocialId(provider, socialId)
                .map(userProvider -> userProvider.getUser().getRole())
                .map(userRole -> userRole.getRoleId())
                .orElseGet(() -> getPendingSignupRole(identity));

        String newAccessToken = jwtProvider.createAccessToken(socialId, provider, role);
        String newRefreshToken = jwtProvider.createRefreshToken(socialId, provider);

        if (!redisTokenRepository.rotateRefreshToken(identity, refreshToken, newRefreshToken)) {
            throw new BaseException(INVALID_TOKEN);
        }

        return new TokenResponseDto(newAccessToken, newRefreshToken);
    }

    /**
     * OAuth2 로그인 성공 후 JWT를 바로 주지 않고 임시 코드(UUID)로 교환해서 반환.
     * 프론트가 URL에서 이 코드를 꺼내 재요청하면 exchangeTemporaryCode()에서 JWT를 발급.
     */
    public String issueTemporaryCode(String provider, String socialId, String role) {
        return issueTemporaryCode(provider, socialId, role, null);
    }

    @Deprecated
    public String issueTemporaryCode(String socialId, String role) {
        return issueTemporaryCode("LEGACY", socialId, role);
    }

    @Deprecated
    public String issueTemporaryCode(String socialId, String role, OAuthSignupInfoDto signupInfo) {
        return issueTemporaryCode(signupInfo.provider(), signupInfo.socialId(), role, signupInfo);
    }

    public String issueTemporaryCode(String provider, String socialId, String role, OAuthSignupInfoDto signupInfo) {
        // JWT를 먼저 만들어서
        String accessToken = jwtProvider.createAccessToken(socialId, provider, role);
        String refreshToken = jwtProvider.createRefreshToken(socialId, provider);

        // UUID를 키로 Redis에 잠깐 저장 — TTL은 RedisTokenRepositoryImpl에서 관리
        String tempCode = UUID.randomUUID().toString();
        redisTokenRepository.saveTemporaryCode(tempCode, new TokenResponseDto(accessToken, refreshToken));
        if (signupInfo != null) {
            redisTokenRepository.saveOAuthSignupInfo(identity(provider, socialId), signupInfo);
        }

        // 프론트에게는 UUID만 전달
        return tempCode;
    }

    public void completeSignup(String provider, String socialId, String nickname, String profileImageUrl, Map<String, Object> domain) {
        String identity = identity(provider, socialId);
        OAuthSignupInfoDto signupInfo = redisTokenRepository.getOAuthSignupInfo(identity);
        if (signupInfo == null) {
            throw new BaseException(INVALID_TEMP_CODE);
        }

        googleOAuth2Service.completeSignup(signupInfo, nickname, profileImageUrl, domain);
        redisTokenRepository.deleteOAuthSignupInfo(identity);
    }

    /**
     * 프론트가 UUID를 들고 재요청하면 Redis에서 JWT를 꺼내서 반환.
     * 일회성 코드이므로 사용 즉시 삭제 — 재사용 시 예외 발생.
     */
    public TokenResponseDto exchangeTemporaryCode(String tempCode) {
        TokenResponseDto tokens = redisTokenRepository.getTemporaryCode(tempCode);

        if (tokens == null) {
            throw new BaseException(INVALID_TEMP_CODE);
        }

        String socialId = jwtProvider.extractSocialId(tokens.getAccessToken());
        String provider = jwtProvider.extractProvider(tokens.getAccessToken());
        redisTokenRepository.saveRefreshToken(identity(provider, socialId), tokens.getRefreshToken());

        // 사용 즉시 삭제 — 일회성 보장
        redisTokenRepository.deleteTemporaryCode(tempCode);

        return tokens;
    }

    public void logout(String provider, String socialId){
        redisTokenRepository.deleteRefreshToken(identity(provider, socialId));
    }

    @Deprecated
    public void logout(String socialId) {
        logout("LEGACY", socialId);
    }

    private String getPendingSignupRole(String identity) {
        OAuthSignupInfoDto signupInfo = redisTokenRepository.getOAuthSignupInfo(identity);
        if (signupInfo == null) {
            throw new BaseException(INVALID_TOKEN);
        }
        return signupInfo.role();
    }

    private String identity(String provider, String socialId) {
        if (provider == null || provider.isBlank() || socialId == null || socialId.isBlank()) {
            throw new BaseException(INVALID_TOKEN);
        }
        return provider + ":" + socialId;
    }
}
