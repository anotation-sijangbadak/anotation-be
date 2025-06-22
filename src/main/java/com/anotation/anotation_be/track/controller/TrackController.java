package com.anotation.anotation_be.track.controller;

import com.anotation.anotation_be.common.dto.emotion.GPTEmotionReqDto;
import com.anotation.anotation_be.common.dto.global.CommonResponse;
import com.anotation.anotation_be.common.dto.global.TokenUserInfo;
import com.anotation.anotation_be.common.enums.CommonStatus;
import com.anotation.anotation_be.common.enums.ErrorCode;
import com.anotation.anotation_be.track.dto.SimpleTrackDto;
import com.anotation.anotation_be.track.dto.TrackInfoDto;
import com.anotation.anotation_be.track.service.TrackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/track")
@RequiredArgsConstructor
public class TrackController {
    private final TrackService trackService;

    @PostMapping("/test")
    public ResponseEntity<?> test(@RequestBody GPTEmotionReqDto reqDto){
        List<SimpleTrackDto> resDto = trackService.recommendMusicCaching(reqDto);
        return new ResponseEntity<>(CommonResponse.ok(CommonStatus.SUCCESS, resDto), HttpStatus.OK);
    }

    @GetMapping("/getTrack")
    public ResponseEntity<?> getTrack(@AuthenticationPrincipal TokenUserInfo userInfo){
        TrackInfoDto resDto = trackService.getTrack(userInfo);
        if (resDto == null){
            return new ResponseEntity<>(CommonResponse.ok(CommonStatus.EMPTY), HttpStatus.OK);
        } else {
            if(resDto.getIndex() == -1) {
                return new ResponseEntity<>(CommonResponse.fail(ErrorCode.INTERNAL_ERROR, "GPT 신뢰도에 문제 발생. 강하늘에게 문의하세요. 다시 요청하면 될지도."), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity<>(CommonResponse.ok(CommonStatus.SUCCESS, resDto), HttpStatus.OK);
        }
    }



}
