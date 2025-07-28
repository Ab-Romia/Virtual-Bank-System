package com.virtualbank.bff_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class WebClientConfig {

    @Value("${transaction.service.url}")
    private String transactionServiceUrl;

    @Value("${user.service.url}")
    private String userServiceUrl;

    @Value("${account.service.url}")
    private String accountServiceUrl;

    @Bean
    public WebClient transactionServiceWebClient() {
        return WebClient.builder()
                .baseUrl(transactionServiceUrl)
                .build();
    }

    @Bean
    public WebClient userServiceWebClient() {
        return WebClient.builder()
                .baseUrl(userServiceUrl)
                .build();
    }

    @Bean
    public WebClient accountServiceWebClient() {
        return WebClient.builder()
                .baseUrl(accountServiceUrl)
                .build();
    }
}