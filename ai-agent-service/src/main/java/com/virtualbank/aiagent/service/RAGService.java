package com.virtualbank.aiagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class RAGService {

    @Value("${user.service.url}")
    private String userServiceUrl;

    @Value("${account.service.url}")
    private String accountServiceUrl;

    @Value("${transaction.service.url}")
    private String transactionServiceUrl;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public RAGService(WebClient webClient) {
        this.webClient = webClient;
        this.objectMapper = new ObjectMapper();
    }

    public String buildUserContext(String userId) {
        StringBuilder context = new StringBuilder();

        try {
            String userInfo = getUserInfo(userId);
            String accountsInfo = getAccountsInfo(userId);

            context.append("User Information:\n");
            context.append(userInfo).append("\n\n");

            context.append("Accounts Information:\n");
            context.append(accountsInfo).append("\n");

        } catch (Exception e) {
            context.append("Limited user data available.\n");
        }

        return context.toString();
    }

    private String getUserInfo(String userId) {
        try {
            String response = webClient.get()
                    .uri(userServiceUrl + "/users/" + userId + "/profile")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response != null) {
                JsonNode userNode = objectMapper.readTree(response);
                return String.format("Name: %s %s, Email: %s",
                        userNode.get("firstName").asText(),
                        userNode.get("lastName").asText(),
                        userNode.get("email").asText());
            }
        } catch (Exception e) {
            return "User profile unavailable";
        }
        return "No user information";
    }

    private String getAccountsInfo(String userId) {
        try {
            String response = webClient.get()
                    .uri(accountServiceUrl + "/users/" + userId + "/accounts")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response != null) {
                JsonNode accountsNode = objectMapper.readTree(response);
                List<String> accountDetails = new ArrayList<>();

                for (JsonNode account : accountsNode) {
                    String accountType = account.get("accountType").asText();
                    String accountNumber = account.get("accountNumber").asText();
                    double balance = account.get("balance").asDouble();
                    String accountId = account.get("accountId").asText();

                    accountDetails.add(String.format("%s Account (%s): Balance $%.2f",
                            accountType, accountNumber, balance));

                    String transactions = getRecentTransactions(accountId);
                    if (!transactions.isEmpty()) {
                        accountDetails.add("  Recent transactions: " + transactions);
                    }
                }

                return accountDetails.isEmpty() ? "No accounts found" : String.join("\n", accountDetails);
            }
        } catch (Exception e) {
            return "Account information unavailable";
        }
        return "No accounts";
    }

    private String getRecentTransactions(String accountId) {
        try {
            String response = webClient.get()
                    .uri(transactionServiceUrl + "/accounts/" + accountId + "/transactions")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response != null) {
                JsonNode transactionsNode = objectMapper.readTree(response);
                List<String> transactionDetails = new ArrayList<>();

                int count = 0;
                for (JsonNode transaction : transactionsNode) {
                    if (count >= 3) break;

                    double amount = transaction.get("amount").asDouble();
                    String description = transaction.has("description") ?
                            transaction.get("description").asText() : "Transfer";

                    transactionDetails.add(String.format("$%.2f (%s)", amount, description));
                    count++;
                }

                return transactionDetails.isEmpty() ? "" : String.join(", ", transactionDetails);
            }
        } catch (Exception e) {
            return "";
        }
        return "";
    }
}
