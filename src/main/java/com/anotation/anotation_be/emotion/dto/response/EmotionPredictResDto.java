package com.anotation.anotation_be.emotion.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class EmotionPredictResDto {
    private List<Result> result;

    @Data
    public static class Result {
        private String label;
        private Double score;
    }
}
