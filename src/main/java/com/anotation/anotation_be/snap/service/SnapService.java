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
        /*
            TODO 회의 안건

            현재 상황 요약
            1. Redis에 이메일이랑 인덱스 달고 있는 채로 TrackInfoDto 저장되어있음
            2. 사용자가 고른 index, description만 전달
            3. 우리는 trace랑 track을 snap으로 저장해야 함

            해결해야 하는 점
            1. trace 어케 가져올거냐? -> DB에서 최신순으로 가져오면 그만
            2. snap은 무슨 정보를 전달할거냐? -> 이건 진짜 모름

            여기부터는 상의하고 하자 ㅋㅋ
         */
    }
}
