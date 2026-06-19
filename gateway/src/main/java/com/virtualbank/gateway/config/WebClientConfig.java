package com.virtualbank.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Reactive client used by the dashboard aggregator to call user-service. The base
 * URL defaults to the local user-service and can be overridden per environment.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient userServiceWebClient(
            WebClient.Builder builder,
            @Value("${USER_SERVICE_URI:http://localhost:8081}") String userServiceUri) {
        return builder.baseUrl(userServiceUri).build();
    }

}
