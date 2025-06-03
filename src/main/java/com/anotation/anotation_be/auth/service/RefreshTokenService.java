package com.anotation.anotation_be.auth.service;

import com.anotation.anotation_be.common.enums.ErrorCode;
import com.anotation.anotation_be.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String PREFIX = "RT:";

    @Value("${jwt.refresh-token-expiration}")
    private Long expiration;

    /**
     * Redis에 Refresh 토큰을 저장하는 메서드
     *
     * @param email Users의 id 값을 String 값으로 변환 후 전달해주세요.
     */
    public void saveRefreshToken(String email, String refreshToken) {
        String key = PREFIX + email;
        redisTemplate.opsForValue().set(key, refreshToken, Duration.ofMillis(expiration));
    }

    /**
     * Redis에 Refresh 토큰을 조회하는 메서드
     *
     * @param email Users의 id 값을 String 값으로 변환 후 전달해주세요.
     * @return 조회 결과가 없을 경우 NO_TOKEN 예외 발생
     */
    public String getRefreshToken(String email) {
        String key = PREFIX + email;
        String value = redisTemplate.opsForValue().get(key);
        if(value == null) {
            throw new BusinessException(ErrorCode.NO_TOKEN);
        }

        return value;
    }

    /**
     * Redis에 Refresh 토큰을 제거하는 메서드
     *
     * @param userId Users의 id 값을 String 값으로 변환 후 전달해주세요.
     */
    public void deleteRefreshToken(String userId) {
        String key = PREFIX + userId;
        redisTemplate.delete(key);
    }
}