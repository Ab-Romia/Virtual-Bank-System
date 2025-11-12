package com.virtualbank.aiagent.service;

import com.virtualbank.aiagent.dto.OpenAIRequest;
import com.virtualbank.aiagent.dto.OpenAIResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class OpenAIService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.model}")
    private String model;

    private final WebClient webClient;

    public OpenAIService(WebClient webClient) {
        this.webClient = webClient;
    }

    public String chat(String systemPrompt, String userMessage) {
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("OpenAI API key is not configured");
            return "AI assistant is not configured. Please set OPENAI_API_KEY environment variable.";
        }

        List<OpenAIRequest.Message> messages = new ArrayList<>();
        messages.add(new OpenAIRequest.Message("system", systemPrompt));
        messages.add(new OpenAIRequest.Message("user", userMessage));

        OpenAIRequest request = new OpenAIRequest();
        request.setModel(model);
        request.setMessages(messages);
        request.setTemperature(0.7);
        request.setMax_tokens(500);

        try {
            System.out.println("Sending request to OpenAI with model: " + model);
            OpenAIResponse response = webClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> {
                                System.err.println("OpenAI API error: " + clientResponse.statusCode());
                                return clientResponse.createException();
                            })
                    .bodyToMono(OpenAIResponse.class)
                    .block();

            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                String content = response.getChoices().get(0).getMessage().getContent();
                if (content != null && !content.isEmpty()) {
                    return content;
                }
            }
        } catch (Exception e) {
            System.err.println("Error calling OpenAI API: " + e.getMessage());
            e.printStackTrace();
            return "I'm having trouble connecting to the AI service. Please try again later. Error: " + e.getMessage();
        }

        return "I couldn't generate a response. Please try again.";
    }
}
