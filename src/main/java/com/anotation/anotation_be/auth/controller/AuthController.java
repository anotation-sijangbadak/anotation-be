package com.anotation.anotation_be.auth.controller;

import com.anotation.anotation_be.auth.dto.request.LoginRequestDto;
import com.anotation.anotation_be.auth.dto.request.SignupReqDto;
import com.anotation.anotation_be.auth.dto.response.LoginResponseDto;
import com.anotation.anotation_be.auth.dto.response.UserIdResDto;
import com.anotation.anotation_be.auth.service.AuthService;
import com.anotation.anotation_be.common.dto.global.CommonResponse;
import com.anotation.anotation_be.common.enums.CommonStatus;
import com.anotation.anotation_be.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "회원 가입")
    @ApiResponse(
            responseCode = "201",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserIdResDto.class))
    )
    @PostMapping("/signup")
    public ResponseEntity<?> singup(@RequestBody @Valid SignupReqDto reqDto) throws BusinessException {
        UserIdResDto resDto = authService.signup(reqDto);
        return new ResponseEntity<>(CommonResponse.ok(CommonStatus.CREATED, resDto), HttpStatus.CREATED);
    }

    @Operation(summary = "로그인")
    @ApiResponse(
            responseCode = "200",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponseDto.class))
    )
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequestDto reqDto) throws BusinessException {
        LoginResponseDto resDto = authService.login(reqDto);
        return new ResponseEntity<>(CommonResponse.ok(CommonStatus.SUCCESS, resDto), HttpStatus.OK);
    }

    @Operation(summary = "로그아웃")
    @ApiResponse(responseCode = "200")
    @GetMapping("/logout/{email}")
    public ResponseEntity<?> logout(@PathVariable String email) throws BusinessException {
        authService.logout(email);
        return new ResponseEntity<>(CommonResponse.ok(CommonStatus.SUCCESS), HttpStatus.OK);
    }

}
