package com.anotation.anotation_be.common.dto.emotion;

import com.anotation.anotation_be.common.enums.EmotionEnum;
import com.anotation.anotation_be.common.enums.Genre;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class GPTEmotionReqDto {
    private String email;
    private String userInput;
    private List<String> emotionList;
    private List<String> genreList;
}
