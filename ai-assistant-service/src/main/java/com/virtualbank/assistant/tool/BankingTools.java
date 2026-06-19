package com.virtualbank.assistant.tool;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

/**
 * Read-only tools the model may call to answer questions about the caller's
 * banking. Every call goes to the other services through a RestClient that
 * forwards the caller's own token, so a tool can only ever read the caller's
 * data: the downstream ownership checks make that the boundary, not this code.
 * Nothing here mutates state, and the model receives only these narrow views.
 */
public class BankingTools {

    private final RestClient accountClient;
    private final RestClient transactionClient;

    public BankingTools(RestClient accountClient, RestClient transactionClient) {
        this.accountClient = accountClient;
        this.transactionClient = transactionClient;
    }

    @Tool(description = "List the caller's own bank accounts with their id, type, balance, currency and status.")
    public List<AccountView> listMyAccounts() {
        return accountClient.get()
                .uri("/accounts")
                .retrieve()
                .body(new ParameterizedTypeReference<List<AccountView>>() {
                });
    }

    @Tool(description = "Get the current balance and details of one of the caller's accounts by its account id.")
    public AccountView getBalance(@ToolParam(description = "The account id to look up") String accountId) {
        return accountClient.get()
                .uri("/accounts/{id}", accountId)
                .retrieve()
                .body(AccountView.class);
    }

    @Tool(description = "List the caller's most recent transfers, including amount, currency, status and the accounts involved.")
    public List<TransferView> recentTransfers() {
        List<TransferView> transfers = transactionClient.get()
                .uri("/transfers")
                .retrieve()
                .body(new ParameterizedTypeReference<List<TransferView>>() {
                });
        if (transfers == null) {
            return List.of();
        }
        // The model only needs a recent window; cap it so the prompt stays small.
        return transfers.stream().limit(10).toList();
    }

    /** The fields of an account the model is allowed to see. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AccountView(
            String id,
            String accountNumber,
            String type,
            BigDecimal balance,
            String currency,
            String status) {
    }

    /** The fields of a transfer the model is allowed to see. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TransferView(
            String transferId,
            String fromAccountId,
            String toAccountId,
            BigDecimal amount,
            String currency,
            String status) {
    }
}
