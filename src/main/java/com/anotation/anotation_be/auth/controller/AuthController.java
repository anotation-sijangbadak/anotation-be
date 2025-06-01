package com.anotation.anotation_be.auth.controller;

import com.anotation.anotation_be.auth.dto.request.LoginRequestDto;
import com.anotation.anotation_be.auth.dto.request.SignupReqDto;
import com.anotation.anotation_be.auth.dto.response.LoginResponseDto;
import com.anotation.anotation_be.auth.dto.response.UserIdResDto;
import com.anotation.anotation_be.auth.service.AuthService;
import com.anotation.anotation_be.common.dto.global.ApiResponse;
import com.anotation.anotation_be.common.enums.CommonStatus;
import com.anotation.anotation_be.common.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<?> singup(@RequestBody @Valid SignupReqDto reqDto) throws BusinessException {
        UserIdResDto resDto = authService.signup(reqDto);
        return new ResponseEntity<>(ApiResponse.ok(CommonStatus.CREATED, resDto), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequestDto reqDto) throws BusinessException {
        LoginResponseDto resDto = authService.login(reqDto);
        return new ResponseEntity<>(ApiResponse.ok(CommonStatus.SUCCESS, resDto), HttpStatus.OK);
    }

}
