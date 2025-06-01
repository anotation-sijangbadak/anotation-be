package com.anotation.anotation_be.auth.service;

import com.anotation.anotation_be.common.constants.MQConstants;
import com.anotation.anotation_be.common.dto.email.EmailReqDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailPublisherService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public void sendEmail(EmailReqDto reqDto) {
        try {
            rabbitTemplate.convertAndSend(MQConstants.SIGNUP_EXCHANGE, MQConstants.SIGNUP_ROUTING_KEY, reqDto);

            log.info("메시지 큐에 정상적으로 이메일 발송 요청이 성공하였습니다.");
        } catch (Exception e) {
            log.warn("메일 발송을 위해 메시징 큐에 전송할 객체 직렬화에 실패하였습니다.");
        }
    }
}