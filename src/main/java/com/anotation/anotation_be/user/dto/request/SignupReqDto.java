package com.anotation.anotation_be.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.validator.constraints.Length;

import java.util.List;

@Getter
@ToString(exclude = "password")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupReqDto {
    @NotBlank
    @Email
    @Schema(defaultValue = "abc123@test.com")
    private String email;

    @NotBlank @Length(min = 8)
    @Schema(defaultValue = "qwer1234!!")
    private String password;

    @NotBlank
    @Schema(defaultValue = "나는문어")
    private String nickname;

    @Schema(defaultValue = "[\"hiphop\", \"jazz\"]")
    private List<String> genre;
}