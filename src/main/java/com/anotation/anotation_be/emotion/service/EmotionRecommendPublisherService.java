package com.anotation.anotation_be.emotion.service;

import com.anotation.anotation_be.common.constants.MQConstants;
import com.anotation.anotation_be.common.dto.emotion.EmotionPredictDto;
import com.anotation.anotation_be.common.dto.emotion.GPTEmotionReqDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmotionRecommendPublisherService {

    private final RabbitTemplate rabbitTemplate;

    public void sendEmotion(GPTEmotionReqDto reqDto) {
        try {
            rabbitTemplate.convertAndSend(MQConstants.EMOTION_SEND_EXCHANGE, MQConstants.EMOTION_SEND_ROUTING_KEY, reqDto);

            log.info("메시지 큐에 정상적으로 이메일 발송 요청이 성공하였습니다.");
        } catch (Exception e) {
            log.warn("음악 추천을 위해 메시징 큐에 전송할 객체 직렬화에 실패하였습니다.");
        }
    }
}