package com.anotation.anotation_be.common.security;

import com.anotation.anotation_be.common.dto.global.CommonResponse;
import com.anotation.anotation_be.common.enums.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        log.warn("인증되지 않은 사용자 접근 : URI - {}", request.getRequestURI());

        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        response.setStatus(errorCode.getStatus());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // 공통 실패 응답 JSON으로 변환
        String body= objectMapper.writeValueAsString(CommonResponse.fail(errorCode));
        response.getWriter().write(body);
    }
}