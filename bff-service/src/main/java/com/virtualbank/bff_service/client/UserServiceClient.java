package com.virtualbank.bff_service.client;

import com.virtualbank.bff_service.dto.UserProfileDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class UserServiceClient {
    private final WebClient userServiceWebClient;

    public UserServiceClient(
            @Qualifier("userServiceWebClient") WebClient userServiceWebClient) {
        this.userServiceWebClient = userServiceWebClient;
    }

    public Mono<UserProfileDto> getUserProfile(UUID userId) {
        return userServiceWebClient.get()
                .uri("/users/{userId}/profile", userId)
                .retrieve()
                .bodyToMono(UserProfileDto.class)
                .onErrorResume(e -> {
                    System.err.println("Error fetching user profile: " + e.getMessage());
                    return Mono.empty();
                });
    }
}