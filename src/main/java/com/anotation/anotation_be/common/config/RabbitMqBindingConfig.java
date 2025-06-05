package com.anotation.anotation_be.common.config;

import com.anotation.anotation_be.common.constants.MQConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqBindingConfig {
    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(MQConstants.SIGNUP_EXCHANGE);
    }

    @Bean
    public Queue userQueue() {
        return new Queue(MQConstants.SIGNUP_QUEUE);
    }

    @Bean
    public Binding signUpBinding() {
        return BindingBuilder
                .bind(userQueue())
                .to(userExchange())
                .with(MQConstants.SIGNUP_ROUTING_KEY);
    }

    @Bean
    public TopicExchange emotionExchange() {
        return new TopicExchange(MQConstants.EMOTION_SEND_EXCHANGE);
    }

    @Bean
    public Queue emotionQueue() {
        return new Queue(MQConstants.EMOTION_SEND_QUEUE);
    }

    @Bean
    public Binding recommendBinding() {
        return BindingBuilder
                .bind(emotionQueue())
                .to(emotionExchange())
                .with(MQConstants.EMOTION_SEND_ROUTING_KEY);
    }
}