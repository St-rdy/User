package com.stardy.user.service;

import com.stardy.user.dto.TokenResponse;
import com.stardy.user.exception.InvalidTokenException;
import com.stardy.user.global.JwtProvider;
import com.stardy.user.repository.RedisTokenRepository;
import com.stardy.user.repository.UserRepository;
import org.springframework.stereotype.Service;

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

    public TokenResponse reissueToken(String refreshToken){
        if(!jwtProvider.isTokenValid(refreshToken)){
            throw new InvalidTokenException("유효하지 않은 토큰입니다");
        }

        String email = jwtProvider.extractEmail(refreshToken);
        String storedToken = redisTokenRepository.getRefreshToken(email);

        if(!refreshToken.equals(storedToken)){
            throw new InvalidTokenException("유효하지 않은 토큰입니다");
        }

        String newAccessToken = jwtProvider.createAccessToken(email, userRepository.getRole(email));
        String newRefreshToken = jwtProvider.createRefreshToken(email);

        redisTokenRepository.deleteRefreshToken(email);
        redisTokenRepository.saveRefreshToken(email, newRefreshToken);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    public void logout(String email){
        redisTokenRepository.deleteRefreshToken(email);
    }
}
