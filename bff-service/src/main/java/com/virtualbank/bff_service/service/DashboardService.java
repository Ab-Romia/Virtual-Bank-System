package com.virtualbank.bff_service.service;

import com.virtualbank.bff_service.client.AccountServiceClient;
import com.virtualbank.bff_service.client.TransactionServiceClient;
import com.virtualbank.bff_service.client.UserServiceClient;
import com.virtualbank.bff_service.dto.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private final UserServiceClient userServiceClient;
    private final AccountServiceClient accountServiceClient;
    private final TransactionServiceClient transactionServiceClient;

    public DashboardService(UserServiceClient userServiceClient,
                            AccountServiceClient accountServiceClient,
                            TransactionServiceClient transactionServiceClient) {
        this.userServiceClient = userServiceClient;
        this.accountServiceClient = accountServiceClient;
        this.transactionServiceClient = transactionServiceClient;
    }

    public Mono<DashboardResponse> getUserDashboard(UUID userId) {
        // Get user profile
        Mono<UserProfileDto> userProfileMono = userServiceClient.getUserProfile(userId);

        // Get user accounts
        Mono<List<UserAccountsResponseDto>> accountsMono = accountServiceClient.getUserAccounts(userId);

        // Combine and enrich with transactions
        return Mono.zip(userProfileMono, accountsMono)
                .flatMap(tuple -> {
                    UserProfileDto userProfile = tuple.getT1();
                    List<UserAccountsResponseDto> accounts = tuple.getT2();

                    // For each account, get its transactions
                    List<Mono<AccountDto>> accountsWithTransactionsMono = accounts.stream()
                            .map(account -> {
                                return transactionServiceClient.getAccountTransactions(account.getAccountId())
                                        .map(transactions -> {
                                            List<TransactionDto> transactionDtos = transactions.stream()
                                                    .map(t -> TransactionDto.builder()
                                                            .transactionId(t.getTransactionId())
                                                            .amount(t.getAmount())
                                                            .fromAccountId(t.getFromAccountId())
                                                            .toAccountId(t.getToAccountId())
                                                            .description(t.getDescription())
                                                            .timestamp(t.getTimestamp())
                                                            .status(t.getStatus())
                                                            .build())
                                                    .collect(Collectors.toList());

                                            return AccountDto.builder()
                                                    .accountId(account.getAccountId())
                                                    .accountNumber(account.getAccountNumber())
                                                    .accountType(account.getAccountType())
                                                    .balance(account.getBalance())
                                                    .transactions(transactionDtos)
                                                    .build();
                                        });
                            })
                            .collect(Collectors.toList());

                    // Combine all accounts with their transactions
                    return Flux.fromIterable(accountsWithTransactionsMono)
                            .flatMap(mono -> mono)
                            .collectList()
                            .map(accountsWithTransactions -> DashboardResponse.builder()
                                    .userId(userProfile.getUserId())
                                    .username(userProfile.getUsername())
                                    .email(userProfile.getEmail())
                                    .firstName(userProfile.getFirstName())
                                    .lastName(userProfile.getLastName())
                                    .accounts(accountsWithTransactions)
                                    .build());
                });
    }
}