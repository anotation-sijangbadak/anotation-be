package com.anotation.anotation_be.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserModifyReqDto {
    private String nickname;
    private List<String> genres;
}
