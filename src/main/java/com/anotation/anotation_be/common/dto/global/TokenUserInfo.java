package com.anotation.anotation_be.common.dto.global;

import lombok.*;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenUserInfo {
    private String email;
    private String role;
}
