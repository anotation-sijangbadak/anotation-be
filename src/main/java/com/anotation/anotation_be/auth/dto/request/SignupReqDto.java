package com.anotation.anotation_be.auth.dto.request;

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
    private String email;
    @NotBlank @Length(min = 8)
    private String password;
    @NotBlank
    private String nickname;

    private List<String> genre;
}