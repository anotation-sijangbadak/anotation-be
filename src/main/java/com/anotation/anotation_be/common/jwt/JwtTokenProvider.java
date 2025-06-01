package com.anotation.anotation_be.common.jwt;

import com.anotation.anotation_be.auth.entity.Users;
import com.anotation.anotation_be.common.enums.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {
    private final Long accessTokenExpiration;
    private final Long refreshTokenExpiration;
    private final byte[] secretKeyBytes;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-token-expiration}") Long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") Long refreshTokenExpiration
    ) {
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.secretKeyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
    }

    public ErrorCode validateToken(String token) {
        try {
            Date expiration = Jwts.parserBuilder()
                    .setSigningKey(secretKeyBytes)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();

            return expiration.after(new Date()) ? null : ErrorCode.TOKEN_EXPIRED;
        } catch (Exception e) {
            log.error("잘못된 토큰 : {}", token);
            return ErrorCode.TOKEN_INVALID;
        }
    }

    public ErrorCode validateRefreshToken(String refreshToken) {
        try {
            Date expiration = Jwts.parserBuilder()
                    .setSigningKey(secretKeyBytes)
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody()
                    .getExpiration();

            return expiration.after(new Date()) ? null : ErrorCode.REFRESH_TOKEN_EXPIRED;
        } catch (Exception e) {
            log.error("잘못된 refresh 토큰 : {}", refreshToken);
            return ErrorCode.TOKEN_INVALID;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKeyBytes)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String createAccessToken(Users user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + accessTokenExpiration))
                .signWith(SignatureAlgorithm.HS512, secretKeyBytes)
                .compact();
    }

    public String createRefreshToken(Users user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + refreshTokenExpiration))
                .signWith(SignatureAlgorithm.HS512, secretKeyBytes)
                .compact();
    }
}