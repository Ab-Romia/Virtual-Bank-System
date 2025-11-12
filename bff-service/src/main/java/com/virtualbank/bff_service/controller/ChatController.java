package com.virtualbank.bff_service.controller;

import com.virtualbank.bff_service.client.AIAgentServiceClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final AIAgentServiceClient aiAgentServiceClient;

    public ChatController(AIAgentServiceClient aiAgentServiceClient) {
        this.aiAgentServiceClient = aiAgentServiceClient;
    }

    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> chat(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String message = request.get("message");

        return aiAgentServiceClient.chat(userId, message)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }
}
