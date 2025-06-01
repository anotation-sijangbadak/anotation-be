package com.anotation.anotation_be.common.dto.email;

import lombok.*;

@Getter @NoArgsConstructor @AllArgsConstructor
@Builder @ToString
public class EmailReqDto {
    private String to;
    private String subject;
    private String body;
}