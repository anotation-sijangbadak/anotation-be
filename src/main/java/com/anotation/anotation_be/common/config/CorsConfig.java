package com.anotation.anotation_be.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // 모든 API
                        .allowedOrigins("*") // 모든 Origin 허용
                        .allowedMethods("*") // GET, POST, PUT, DELETE 등등 허용
                        .allowedHeaders("*"); // 모든 헤더 허용
            }
        };
    }
}
