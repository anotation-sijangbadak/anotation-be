package com.anotation.anotation_be.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TokenReissueResDto {
    @Schema(defaultValue = "재발급된 Access Token...")
    private String token;
}
