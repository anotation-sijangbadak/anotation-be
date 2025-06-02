package com.anotation.anotation_be.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserIdResDto {
    @Schema(defaultValue = "2")
    private Long id;
}