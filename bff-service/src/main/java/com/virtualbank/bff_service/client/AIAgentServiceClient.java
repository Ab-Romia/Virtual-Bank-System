package com.virtualbank.bff_service.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class AIAgentServiceClient {

    private final WebClient webClient;

    public AIAgentServiceClient(@Qualifier("aiAgentServiceWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<Map<String, Object>> chat(String userId, String message) {
        Map<String, String> requestBody = Map.of(
                "userId", userId,
                "message", message
        );

        return webClient.post()
                .uri("/chat")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> {
                            System.err.println("AI Agent Service error: " + clientResponse.statusCode());
                            return clientResponse.createException();
                        })
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .doOnError(error -> System.err.println("Error calling AI Agent Service: " + error.getMessage()));
    }
}
