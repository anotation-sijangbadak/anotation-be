package com.anotation.anotation_be.common.dto.emotion;

import lombok.Data;

import java.util.List;

@Data
public class EmotionPredictDto {
    private List<Result> result;

    @Data
    public static class Result {
        private String label;
        private Double score;
    }
}
