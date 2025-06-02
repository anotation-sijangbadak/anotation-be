package com.anotation.anotation_be.common.jwt;

import com.anotation.anotation_be.common.dto.global.CommonResponse;
import com.anotation.anotation_be.common.dto.global.TokenUserInfo;
import com.anotation.anotation_be.common.enums.ErrorCode;
import com.anotation.anotation_be.common.enums.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    List<String> whiteList = List.of(
            "/auth/signup", "/auth/login", "/swagger-ui/", "/v3/"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.warn(request.getRequestURI());
        for (String path : whiteList) {
            if(request.getRequestURI().equals(path) || request.getRequestURI().startsWith(path)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        log.warn("JWT Filter에 돌고 있어요!");

        String authHeader = request.getHeader("Authorization");

        // 토큰이 존재하는 지 확인
        if (authHeader == null || authHeader.isEmpty() ) {
            log.warn("Authorization 헤더가 비어있습니다.");
            onError(response, ErrorCode.NO_TOKEN);
            return;
        }

        // Bearer 토큰인지 확인
        if (!authHeader.startsWith("Bearer ")) {
            log.warn("Authorization 헤더가 Bearer 형식이 아닙니다.");
            onError(response, ErrorCode.TOKEN_INVALID);
            return;
        }
        String token = authHeader.replace("Bearer ", "");
        // 토큰 유효성 검사
        ErrorCode errorCode = jwtTokenProvider.validateToken(token);
        if (errorCode != null) {
            log.warn("토큰이 유효하지 않습니다. Error: {}", errorCode.getMessage());
            onError(response, errorCode);
            return;
        }

        // 토큰에서 사용자 정보 추출
        Claims claims = jwtTokenProvider.getClaims(token);
        String email = claims.getSubject();
        Role role;
        try {
            role = Role.from(claims.get("role", String.class));
        } catch (RuntimeException e) {
            log.warn("역할이 유효하지 않습니다.");
            onError(response, ErrorCode.ROLE_INVALID);
            return;
        }

        // @AuthenticationPrinciple, @PreAuthorize("hasRole('ADMIN')") 같은 로직을 사용하기 위한 로직
        TokenUserInfo tokenUserInfo = TokenUserInfo.builder().email(email).role(role.name()).build();
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(tokenUserInfo, "", authorities);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        // 문제 없다면 진행
        filterChain.doFilter(request, response);
    }

    private void onError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // 공통 실패 응답 JSON으로 변환
        String body = objectMapper.writeValueAsString(CommonResponse.fail(errorCode));
        response.getWriter().write(body);
    }
}