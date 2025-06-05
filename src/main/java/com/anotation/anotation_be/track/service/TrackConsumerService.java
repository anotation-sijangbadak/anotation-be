package com.anotation.anotation_be.track.service;

import com.anotation.anotation_be.common.dto.email.EmailReqDto;
import com.anotation.anotation_be.common.dto.emotion.EmotionPredictDto;
import com.anotation.anotation_be.email.service.EmailService;
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

    @RabbitListener(queues = "track.recommend.queue")
    public void handleRecommendMessage(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();

        switch (routingKey) {
            case "recommend.emotion.send" :
                recommendMusic(message.getBody());
                break;
        }
    }

    private void recommendMusic(byte[] body) {
        try {
            log.info("MQ 이메일 발송 메시지 수신!");

            EmotionPredictDto reqDto = objectMapper.readValue(body, EmotionPredictDto.class);
            trackService.recommendMusic(reqDto);
        } catch (Exception e) {
            log.warn("MQ 메시지 처리에 실패했습니다.");
            // TODO: DLQ(Dead Letter Queue) 설정 필요
        }
    }


}
