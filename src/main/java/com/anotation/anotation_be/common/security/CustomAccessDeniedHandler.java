package com.anotation.anotation_be.common.security;

import com.anotation.anotation_be.common.dto.global.ApiResponse;
import com.anotation.anotation_be.common.enums.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        log.warn("인가 처리에 실패하였습니다. URI: {}", request.getRequestURI());

        ErrorCode errorCode = ErrorCode.FORBIDDEN;

        response.setStatus(errorCode.getStatus());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // 공통 실패 응답 JSON으로 변환
        String body = objectMapper.writeValueAsString(ApiResponse.fail(errorCode));
        response.getWriter().write(body);
    }
}
