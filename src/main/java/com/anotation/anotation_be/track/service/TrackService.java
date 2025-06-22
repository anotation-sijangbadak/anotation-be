package com.anotation.anotation_be.track.service;

import com.anotation.anotation_be.common.constants.MQConstants;
import com.anotation.anotation_be.common.dto.global.TokenUserInfo;
import com.anotation.anotation_be.common.enums.ErrorCode;
import com.anotation.anotation_be.common.exception.BusinessException;
import com.anotation.anotation_be.track.dto.RedisTrackIndexDto;
import com.anotation.anotation_be.track.dto.SimpleTrackDto;
import com.anotation.anotation_be.common.dto.track.TrackInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrackService {
    //region AutoWired Object
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, SimpleTrackDto> redisSimpleTrackTemplate;
    private final RedisTemplate<String, TrackInfoDto> redisTrackInfoTemplate;
    //endregion
    //region Constants
    private static final String REDIS_INDEX = ":INDEX:";
    private static final String REDIS_SIMPLE_TRACK = "SIMPLE:TRACK:";
    private static final String REDIS_TRACK_INFO = "TRACK:INFO:";

    private static final int COUNT_PER_MQ = 3;
    //endregion

    public TrackInfoDto getTrack(TokenUserInfo userInfo) {
        // 전달할 Track 정보가 있는지 확인
        Set<String> keys = redisTrackInfoTemplate.keys(REDIS_TRACK_INFO + userInfo.getEmail() + REDIS_INDEX + "*");
        if (keys.isEmpty()) {
            Set<String> simpleTrackKeys = redisSimpleTrackTemplate.keys(REDIS_SIMPLE_TRACK + userInfo.getEmail() + REDIS_INDEX + "*");
            if (!simpleTrackKeys.isEmpty()) {
                // 기본 정보 있는 거 하나 바꾸기
                rabbitTemplate.convertAndSend(
                        MQConstants.TRACK_EXCHANGE,
                        MQConstants.TRACK_CACHING_KEY,
                        new RedisTrackIndexDto(userInfo.getEmail(), 0)
                );

                // 3개씩 전파하는데도 실제 데이터가 없었다는 것은 GPT 신뢰도가 바닥이라는 것
                return TrackInfoDto.builder().index(-1).build();
            }

            // 모두 변환했기 때문에 정보가 없음을 전달
            return null;
        }

        // 저장되어 있는 DTO를 꺼내기
        String key = keys.iterator().next();
        TrackInfoDto resDto = redisTrackInfoTemplate.opsForValue().get(key);

        if (resDto == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Redis에서 받아온 정보가 비어있습니다.");
        }

        // 다시 조회되지 않도록 키 이름 변경
        redisTrackInfoTemplate.rename(key, "SEND:INFO:" + userInfo.getEmail() + REDIS_INDEX + resDto.getIndex());

        // 다음으로 계속
        Set<String> simpleTrackKeys = redisSimpleTrackTemplate.keys(REDIS_SIMPLE_TRACK + userInfo.getEmail() + REDIS_INDEX + "*");
        if (!simpleTrackKeys.isEmpty()) {
            for (int i = 0; i < COUNT_PER_MQ; i++) {
                rabbitTemplate.convertAndSend(
                        MQConstants.TRACK_EXCHANGE,
                        MQConstants.TRACK_CACHING_KEY,
                        new RedisTrackIndexDto(userInfo.getEmail(), i)
                );
            }
        }

        return resDto;
    }


}
