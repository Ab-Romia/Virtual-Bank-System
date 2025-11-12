package com.virtualbank.aiagent.service;

import com.virtualbank.aiagent.dto.ChatRequest;
import com.virtualbank.aiagent.dto.ChatResponse;
import org.springframework.stereotype.Service;

@Service
public class AIAgentService {

    private final RAGService ragService;
    private final OpenAIService openAIService;

    public AIAgentService(RAGService ragService, OpenAIService openAIService) {
        this.ragService = ragService;
        this.openAIService = openAIService;
    }

    public ChatResponse processChat(ChatRequest request) {
        String userContext = ragService.buildUserContext(request.getUserId());

        String systemPrompt = buildSystemPrompt(userContext);

        String aiResponse = openAIService.chat(systemPrompt, request.getMessage());

        return new ChatResponse(aiResponse, userContext);
    }

    private String buildSystemPrompt(String userContext) {
        return String.format(
                "You are a helpful banking assistant for Virtual Bank. " +
                "You help users with their banking questions based on their account information. " +
                "Be friendly, professional, and concise.\n\n" +
                "Current user context:\n%s\n\n" +
                "Use this context to provide personalized and accurate responses. " +
                "If asked about specific balances or transactions, refer to the context above. " +
                "If the information is not in the context, politely inform the user.",
                userContext
        );
    }
}
