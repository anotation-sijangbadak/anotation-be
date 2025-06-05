package com.anotation.anotation_be.emotion.controller;

import com.anotation.anotation_be.common.dto.global.CommonResponse;
import com.anotation.anotation_be.common.dto.global.TokenUserInfo;
import com.anotation.anotation_be.common.enums.CommonStatus;
import com.anotation.anotation_be.emotion.dto.request.UserPromptReqDto;
import com.anotation.anotation_be.emotion.dto.response.UserEmotionResDto;
import com.anotation.anotation_be.emotion.service.EmotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/emotion")
@RequiredArgsConstructor
@Slf4j
public class EmotionController {
    private final EmotionService emotionService;

    @GetMapping("/translate")
    public ResponseEntity<?> getEmotion(@AuthenticationPrincipal TokenUserInfo userInfo, @RequestBody UserPromptReqDto reqDto) {
        UserEmotionResDto emotion = emotionService.getEmotion(userInfo, reqDto);
        return new ResponseEntity<>(CommonResponse.ok(CommonStatus.SUCCESS,emotion), HttpStatus.OK);
    }
}
