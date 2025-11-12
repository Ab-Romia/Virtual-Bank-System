package com.virtualbank.aiagent.controller;

import com.virtualbank.aiagent.dto.ChatRequest;
import com.virtualbank.aiagent.dto.ChatResponse;
import com.virtualbank.aiagent.service.AIAgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final AIAgentService aiAgentService;

    public ChatController(AIAgentService aiAgentService) {
        this.aiAgentService = aiAgentService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        try {
            ChatResponse response = aiAgentService.processChat(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ChatResponse errorResponse = new ChatResponse(
                    "I'm sorry, I encountered an error processing your request. Please try again.",
                    ""
            );
            return ResponseEntity.ok(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Agent Service is running");
    }
}
