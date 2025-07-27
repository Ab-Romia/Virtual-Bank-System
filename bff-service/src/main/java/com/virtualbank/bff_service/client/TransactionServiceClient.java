package com.virtualbank.bff_service.client;

import com.virtualbank.bff_service.dto.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

@Component
public class TransactionServiceClient {
    private final WebClient transactionServiceWebClient;

    public TransactionServiceClient(
            @Qualifier("transactionServiceWebClient") WebClient transactionServiceWebClient) {
        this.transactionServiceWebClient = transactionServiceWebClient;
    }

    public Mono<List<TransactionsResponseDto>> getAccountTransactions(UUID accountId) {
        return transactionServiceWebClient.get()
                .uri("/accounts/{accountId}/transactions", accountId)
                .retrieve()
                .bodyToFlux(TransactionsResponseDto.class)
                .doOnNext(t -> {
                    if (t.getAmount().doubleValue() < 0) {
                        t.setFromAccountId(accountId);
                        t.setToAccountId(UUID.randomUUID());
                    } else {
                        t.setToAccountId(accountId);
                        t.setFromAccountId(UUID.randomUUID());
                    }
                })
                .collectList()
                .onErrorResume(e -> {
                    System.err.println("Error fetching transactions: " + e.getMessage());
                    return Mono.just(List.of());
                });
    }
}