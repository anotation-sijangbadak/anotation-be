package com.anotation.anotation_be.common.mq;

import com.anotation.anotation_be.common.constants.MQConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqBindingConfig {
    // AMQP EXCHANGE, QUEUE, ROUTING_KEY 이름 Convention
    // EXCHANGE : (from Domain).exchange
    // QUEUE : (to Domain).queue
    // ROUTING_KEY : (from).(to).(task)

    //region Exchange

    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(MQConstants.USER_EXCHANGE);
    }

    @Bean
    public TopicExchange emotionExchange() {
        return new TopicExchange(MQConstants.EMOTION_EXCHANGE);
    }

    @Bean
    public TopicExchange trackExchange() {
        return new TopicExchange(MQConstants.TRACK_EXCHANGE);
    }

    // endregion

    //region Queue

    @Bean
    public Queue emailQueue() {
        return new Queue(MQConstants.EMAIL_QUEUE);
    }

    @Bean
    public Queue trackQueue() {
        return new Queue(MQConstants.TRACK_QUEUE);
    }

    //endregion

    //region Binding

    @Bean
    public Binding signUpBinding() {
        return BindingBuilder
                .bind(emailQueue())
                .to(userExchange())
                .with(MQConstants.SIGNUP_KEY);
    }

    @Bean
    public Binding trackRecommendBinding() {
        return BindingBuilder
                .bind(trackQueue())
                .to(emotionExchange())
                .with(MQConstants.TRACK_RECOMMEND_KEY);
    }

    @Bean
    public Binding trackCachingBinding() {
        return BindingBuilder
                .bind(trackQueue())
                .to(trackExchange())
                .with(MQConstants.TRACK_CACHING_KEY);
    }

    //endregion
}