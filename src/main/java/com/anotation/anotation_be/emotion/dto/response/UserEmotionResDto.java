package com.anotation.anotation_be.emotion.dto.response;

import com.anotation.anotation_be.common.enums.EmotionEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserEmotionResDto {
    private String translatedText;

    private List<String> emotionList;
    // TODO: track 도 줘야 겠네 여기
}
