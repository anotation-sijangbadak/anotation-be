package com.anotation.anotation_be.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@ToString(exclude = "password")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequestDto {
    @NotBlank
    @Email
    @Schema(defaultValue = "abc123@test.com")
    private String email;

    @NotBlank
    @Schema(defaultValue = "qwer1234!!")
    private String password;
}