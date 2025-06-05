package com.anotation.anotation_be.emotion.dto.response;

import lombok.Data;

@Data
public class TranslateResDto {
    private Message message;

    @Data
    public static class Message {
        private Result result;
    }

    @Data
    public static class Result {
        private String srcLangType;
        private String tarLangType;
        private String translatedText;
    }
}
