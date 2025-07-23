package com.virtualbank.bff_service.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import com.virtualbank.bff_service.dto.AccountResponseDto;
import com.virtualbank.bff_service.dto.UserAccountsResponseDto;
import java.util.List;
import java.util.UUID;

@Component
public class AccountServiceClient {
    private final WebClient accountServiceWebClient;

    public AccountServiceClient(
            @Qualifier("accountServiceWebClient") WebClient accountServiceWebClient) {
        this.accountServiceWebClient = accountServiceWebClient;
    }

    public Mono<AccountResponseDto> getAccount(UUID accountId) {
        return accountServiceWebClient.get()
                .uri("/accounts/{accountId}", accountId)
                .retrieve()
                .bodyToMono(AccountResponseDto.class)
                .onErrorResume(e -> {
                    System.err.println("Error fetching account: " + e.getMessage());
                    return Mono.empty();
                });
    }

    public Mono<List<UserAccountsResponseDto>> getUserAccounts(UUID userId) {
        return accountServiceWebClient.get()
                .uri("/users/{userId}/accounts", userId)
                .retrieve()
                .bodyToFlux(UserAccountsResponseDto.class)
                .collectList()
                .onErrorResume(e -> {
                    System.err.println("Error fetching user accounts: " + e.getMessage());
                    return Mono.just(List.of());
                });
    }
}