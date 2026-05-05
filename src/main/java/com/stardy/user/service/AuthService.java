package com.stardy.user.service;

import com.stardy.user.dto.TokenResponseDto;
import com.stardy.user.entity.User;
import com.stardy.user.exception.BaseException;
import com.stardy.user.global.JwtProvider;
import com.stardy.user.repository.RedisTokenRepository;
import com.stardy.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.stardy.user.exception.ErrorCode.*;

@Service
public class AuthService {

    private final JwtProvider jwtProvider;
    private final RedisTokenRepository redisTokenRepository;
    private final UserRepository userRepository;

    public AuthService(JwtProvider jwtProvider, RedisTokenRepository redisTokenRepository, UserRepository userRepository) {
        this.jwtProvider = jwtProvider;
        this.redisTokenRepository = redisTokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public TokenResponseDto reissueToken(String refreshToken){
        if(!jwtProvider.isTokenValid(refreshToken)){
            throw new BaseException(INVALID_TOKEN);
        }

        String email = jwtProvider.extractEmail(refreshToken);
        String storedToken = redisTokenRepository.getRefreshToken(email);

        if(!refreshToken.equals(storedToken)){
            throw new BaseException(INVALID_TOKEN);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(INVALID_TOKEN));

        if (user.getRole() == null) {
            throw new BaseException(INVALID_TOKEN);
        }

        String role = user.getRole().getRoleId();

        String newAccessToken = jwtProvider.createAccessToken(email, role);
        String newRefreshToken = jwtProvider.createRefreshToken(email);

        redisTokenRepository.deleteRefreshToken(email);
        redisTokenRepository.saveRefreshToken(email, newRefreshToken);

        return new TokenResponseDto(newAccessToken, newRefreshToken);
    }

    /**
     * OAuth2 로그인 성공 후 JWT를 바로 주지 않고 임시 코드(UUID)로 교환해서 반환.
     * 프론트가 URL에서 이 코드를 꺼내 재요청하면 exchangeTemporaryCode()에서 JWT를 발급.
     *
     * 이렇게 한 번 더 꼬는 이유는 보안 때문이야.
     * 콜백 URL에 JWT를 직접 담으면 브라우저 히스토리나 서버 로그에 토큰이 남을 수 있어.
     * UUID는 그 자체로 아무 정보도 없고, 30초 안에 사용하지 않으면 만료돼.
     */
    public String issueTemporaryCode(String email, String role) {
        // JWT를 먼저 만들어서
        String accessToken = jwtProvider.createAccessToken(email, role);
        String refreshToken = jwtProvider.createRefreshToken(email);

        // UUID를 키로 Redis에 잠깐 저장 — TTL은 RedisTokenRepositoryImpl에서 관리
        String tempCode = UUID.randomUUID().toString();
        redisTokenRepository.saveTemporaryCode(tempCode, new TokenResponseDto(accessToken, refreshToken));

        // 프론트에게는 UUID만 전달
        return tempCode;
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

        // 사용 즉시 삭제 — 일회성 보장
        redisTokenRepository.deleteTemporaryCode(tempCode);

        return tokens;
    }

    public void logout(String email){
        redisTokenRepository.deleteRefreshToken(email);
    }
}
