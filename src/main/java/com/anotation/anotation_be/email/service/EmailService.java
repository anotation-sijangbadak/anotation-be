package com.anotation.anotation_be.email.service;

import com.anotation.anotation_be.common.dto.email.EmailReqDto;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final JavaMailSender mailSender;
    private final RabbitTemplate rabbitTemplate;

    public void sendEmail(EmailReqDto reqDto) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            helper.setTo(reqDto.getTo());
            helper.setSubject(reqDto.getSubject());
            helper.setText(reqDto.getBody(), true);

            mailSender.send(mimeMessage);

            log.info("이메일을 성공적으로 전송하였습니다. to : {}", reqDto.getTo());
        } catch (MessagingException e) {
            log.warn("이메일 서버가 동작하지 않습니다.");
//            throw new BusinessException(ErrorCode.EMAIL_SERVER_ERROR);
        } catch (RuntimeException e) {
            log.warn("이메일 전송에 실패하였습니다. message : {}", e.getMessage());
//            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

}