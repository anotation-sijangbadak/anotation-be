package com.anotation.anotation_be.auth.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserModifyReqDto {
    private String nickname;
    private List<String> genres;
}
