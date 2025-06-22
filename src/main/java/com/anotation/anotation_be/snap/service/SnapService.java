package com.anotation.anotation_be.snap.service;

import com.anotation.anotation_be.common.dto.global.TokenUserInfo;
import com.anotation.anotation_be.common.dto.track.TrackInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SnapService {
    public void selectTrack(TokenUserInfo userInfo, int index) {
        TrackInfoDto dto;
    }
}
