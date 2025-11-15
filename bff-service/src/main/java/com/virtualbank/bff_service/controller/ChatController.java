package com.virtualbank.bff_service.controller;

import com.virtualbank.bff_service.client.AIAgentServiceClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/bff/chat")
public class ChatController {

    private final AIAgentServiceClient aiAgentServiceClient;

    public ChatController(AIAgentServiceClient aiAgentServiceClient) {
        this.aiAgentServiceClient = aiAgentServiceClient;
    }

    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> chat(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String message = request.get("message");

        if (userId == null || userId.isEmpty() || message == null || message.isEmpty()) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("message", "userId and message are required")));
        }

        return aiAgentServiceClient.chat(userId, message)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    System.err.println("Error in chat endpoint: " + error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(Map.of("message", "I'm sorry, I encountered an error. Please try again.")));
                });
    }
}
