package com.anotation.anotation_be.emotion.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmotionPredictReqDto {
    private String text;
}
