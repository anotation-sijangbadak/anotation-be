package com.anotation.anotation_be.common.config;

import com.anotation.anotation_be.track.dto.SimpleTrackDto;
import com.anotation.anotation_be.common.dto.track.TrackInfoDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory();
    }

    /**
     * 문자열 타입(토큰)을 저장하기 위한 RedisTemplate
     * Value 직렬화 - StringRedisSerializer
     */
    @Bean @Primary
    public RedisTemplate<String, String> redisStringTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());

        return template;
    }

    /**
     * 객체 타입을 저장하기 위한 RedisTemplate
     * Value 직렬화 - GenericJackson2JsonRedisSerializer
     */
    @Bean
    public RedisTemplate<String, SimpleTrackDto> redisSimpleTrackTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, SimpleTrackDto> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }

    /**
     * 객체 타입을 저장하기 위한 RedisTemplate
     * Value 직렬화 - GenericJackson2JsonRedisSerializer
     */
    @Bean
    public RedisTemplate<String, TrackInfoDto> redisTrackInfoTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, TrackInfoDto> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }

    /**
     * 객체 타입을 저장하기 위한 RedisTemplate
     * Value 직렬화 - GenericJackson2JsonRedisSerializer
     */
    @Bean
    public RedisTemplate<String, Object> redisObjectTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }
}
