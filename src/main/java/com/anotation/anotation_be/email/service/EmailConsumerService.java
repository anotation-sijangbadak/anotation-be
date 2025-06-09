package com.anotation.anotation_be.email.service;

import com.anotation.anotation_be.common.dto.email.EmailReqDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailConsumerService {
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "email.queue")
    public void handleEmailMessage(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();

        switch (routingKey) {
            case "user.email.signup" :
                sendMessage(message.getBody());
                break;
        }
    }

    private void sendMessage(byte[] body) {
        try {
            log.info("MQ 이메일 발송 메시지 수신!");

            EmailReqDto reqDto = objectMapper.readValue(body, EmailReqDto.class);
            emailService.sendEmail(reqDto);
        } catch (Exception e) {
            log.warn("MQ 메시지 처리에 실패했습니다.");
        }
    }


}
