package com.anotation.anotation_be.track.service;

import com.anotation.anotation_be.common.dto.email.EmailReqDto;
import com.anotation.anotation_be.common.dto.emotion.EmotionPredictDto;
import com.anotation.anotation_be.common.dto.emotion.GPTEmotionReqDto;
import com.anotation.anotation_be.common.exception.BusinessException;
import com.anotation.anotation_be.email.service.EmailService;
import com.anotation.anotation_be.track.dto.RedisTrackIndexDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrackConsumerService {
    private final TrackService trackService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "track.queue")
    public void handleRecommendMessage(Message message) throws InterruptedException {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();

        switch (routingKey) {
            case "emotion.track.recommend" :
                recommendMusicCaching(message.getBody());
                break;
            case "track.track.caching" :
                cacheTrackInfo(message.getBody());
                break;
        }
    }

    private void recommendMusicCaching(byte[] body) {
        try {
            log.info("MQ 음악 추천 메시지 수신!");

            GPTEmotionReqDto reqDto = objectMapper.readValue(body, GPTEmotionReqDto.class);
            trackService.recommendMusicCaching(reqDto);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private void cacheTrackInfo(byte[] body) throws InterruptedException {
        RedisTrackIndexDto reqDto;
        try {
            reqDto = objectMapper.readValue(body, RedisTrackIndexDto.class);
        } catch (IOException e) {
            log.error(e.getMessage());
            return;
        }

        try {
            log.info("MQ 트랙 정보 캐싱 메시지 수신!");

            trackService.recommendTrackInfoCaching(reqDto);
        } catch (BusinessException e) {
            log.error(e.getMessage());
            if(e.getErrorCode().getStatus() == 503) {
                // 4XX 에러 발생
                Thread.sleep(1000);
                // TODO 이거 계속 처리해줘야 하지 않을까..?
                trackService.recommendTrackInfoCaching(reqDto);
            }
        }
    }
}
