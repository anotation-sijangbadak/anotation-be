package com.anotation.anotation_be.emotion.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public class TranslateReqDto {
    private String source;
    private String target;
    private String text;

    public TranslateReqDto(String text) {
        this.source = "ko";
        this.target = "en";
        this.text = text;
    }
}
