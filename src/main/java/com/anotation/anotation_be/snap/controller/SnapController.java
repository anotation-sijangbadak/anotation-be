package com.anotation.anotation_be.snap.controller;

import com.anotation.anotation_be.common.dto.global.TokenUserInfo;
import com.anotation.anotation_be.snap.service.SnapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/snap")
@RequiredArgsConstructor
@Slf4j
public class SnapController {
    private final SnapService snapService;

    @PostMapping("/selectTrack/{index}")
    public ResponseEntity<?> selectTrack(@AuthenticationPrincipal TokenUserInfo userInfo, @PathVariable int index){
        // 스냅 생성
        snapService.selectTrack(userInfo, index);

        return null;
    }
}
