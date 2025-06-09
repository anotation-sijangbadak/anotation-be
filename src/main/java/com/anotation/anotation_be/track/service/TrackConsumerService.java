package com.anotation.anotation_be.track.service;

import com.anotation.anotation_be.common.dto.email.EmailReqDto;
import com.anotation.anotation_be.common.dto.emotion.EmotionPredictDto;
import com.anotation.anotation_be.common.dto.emotion.GPTEmotionReqDto;
import com.anotation.anotation_be.email.service.EmailService;
import com.anotation.anotation_be.track.dto.RedisTrackIndexDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrackConsumerService {
    private final TrackService trackService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "track.queue")
    public void handleRecommendMessage(Message message) {
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
            log.error(e.getMessage(), e);
        }
    }

    private void cacheTrackInfo(byte[] body) {
        try {
            log.info("MQ 트랙 정보 캐싱 메시지 수신!");

            RedisTrackIndexDto reqDto = objectMapper.readValue(body, RedisTrackIndexDto.class);
            trackService.recommendTrackInfoCaching(reqDto);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
