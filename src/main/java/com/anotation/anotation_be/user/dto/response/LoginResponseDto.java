package com.anotation.anotation_be.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginResponseDto {
    @Schema(defaultValue = "Access Token...")
    private String accessToken;

    @Schema(defaultValue = "Refresh Token...")
    private String refreshToken;

    @Schema(defaultValue = "2")
    private Long userId;

    @Schema(defaultValue = "나는문어")
    private String nickname;
}