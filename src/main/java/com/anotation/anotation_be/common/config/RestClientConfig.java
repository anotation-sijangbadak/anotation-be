package com.anotation.anotation_be.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {
    private final RestClient.Builder restClientBuilder;

    @Bean
    public RestClient restClient() {
        return restClientBuilder.build();
    }
}
