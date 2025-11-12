package com.virtualbank.bff_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class AIAgentServiceClient {

    private final WebClient webClient;

    @Value("${ai.agent.service.url}")
    private String aiAgentServiceUrl;

    public AIAgentServiceClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<Map<String, Object>> chat(String userId, String message) {
        Map<String, String> requestBody = Map.of(
                "userId", userId,
                "message", message
        );

        return webClient.post()
                .uri(aiAgentServiceUrl + "/chat")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class);
    }
}
