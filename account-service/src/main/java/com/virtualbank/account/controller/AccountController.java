package com.virtualbank.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualbank.account.dto.*;
import com.virtualbank.account.exception.InvalidCreationException;
import com.virtualbank.account.exception.UserNotFoundException;
import com.virtualbank.account.scheduler.AccountInactiveJob;
import com.virtualbank.account.service.AccountService;
import com.virtualbank.account.service.KafkaProducerService;
import jakarta.validation.Valid;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class AccountController {
    private final AccountService accountService;
    private final KafkaProducerService kafkaProducerService;
    private final ObjectMapper objectMapper;


    public AccountController(AccountService accountService,
                             KafkaProducerService kafkaProducerService,
                             ObjectMapper objectMapper) {
        this.accountService = accountService;
        this.kafkaProducerService = kafkaProducerService;
        this.objectMapper = objectMapper;
    }

    @PutMapping("/accounts/transfer")
    public ResponseEntity<TransferResponse> transferFunds(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(name = "APP-NAME", required = false, defaultValue = "UNKNOWN") String appName) {

        String endpoint = "/accounts/transfer";
        sendLogToKafka("Request", endpoint, request, appName);

        TransferResponse response = accountService.transferFunds(request);

        sendLogToKafka("Response", endpoint, response, appName);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable String accountId,
            @RequestHeader(name = "APP-NAME", required = false, defaultValue = "UNKNOWN") String appName) {

        String endpoint = "/accounts/" + accountId;
        sendLogToKafka("Request", endpoint, "Get account with ID: " + accountId, appName);

        try {
            UUID id = UUID.fromString(accountId);
            AccountResponse account = accountService.getAccount(id);

            sendLogToKafka("Response", endpoint, account, appName);
            return ResponseEntity.ok(account);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", 400);
            errorResponse.put("error", "Bad Request");
            errorResponse.put("message", "Invalid account ID format: " + accountId);

            sendLogToKafka("Response", endpoint, errorResponse, appName);
            throw new com.virtualbank.account.exception.AccountNotFoundException(
                    "Invalid account ID format: " + accountId);
        }
    }

    @PostMapping("/accounts")
    public ResponseEntity<?> createAccount(
            @Valid @RequestBody CreateRequest request,
            @RequestHeader(name = "APP-NAME", required = false, defaultValue = "UNKNOWN") String appName) {

        String endpoint = "/accounts";
        sendLogToKafka("Request", endpoint, request, appName);

        try {
            CreateResponse response = accountService.createAccount(request);

            sendLogToKafka("Response", endpoint, response, appName);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (UserNotFoundException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", 404);
            errorResponse.put("error", "Not Found");
            errorResponse.put("message", e.getMessage());

            sendLogToKafka("Response", endpoint, errorResponse, appName);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", 400);
            errorResponse.put("error", "Bad Request");
            errorResponse.put("message", "Invalid account type or initial balance");

            sendLogToKafka("Response", endpoint, errorResponse, appName);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/accounts/number/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByNumber(
            @PathVariable String accountNumber,
            @RequestHeader(name = "APP-NAME", required = false, defaultValue = "UNKNOWN") String appName) {

        String endpoint = "/accounts/number/" + accountNumber;
        sendLogToKafka("Request", endpoint, "Get account with number: " + accountNumber, appName);

        AccountResponse account = accountService.getAccountByAccountNumber(accountNumber);

        sendLogToKafka("Response", endpoint, account, appName);
        return ResponseEntity.ok(account);
    }

    @GetMapping("/users/{userId}/accounts")
    public ResponseEntity<List<UserAccountsResponse>> findUserAccounts(
            @PathVariable String userId,
            @RequestHeader(name = "APP-NAME", required = false, defaultValue = "UNKNOWN") String appName) {

        String endpoint = "/users/" + userId + "/accounts";
        sendLogToKafka("Request", endpoint, "Get accounts for user: " + userId, appName);

        try {
            UUID id = UUID.fromString(userId);
            List<UserAccountsResponse> response = accountService.findUserAccounts(id);

            sendLogToKafka("Response", endpoint, response, appName);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", 400);
            errorResponse.put("error", "Bad Request");
            errorResponse.put("message", "Invalid user ID format: " + userId);

            sendLogToKafka("Response", endpoint, errorResponse, appName);
            throw new com.virtualbank.account.exception.AccountNotFoundException(
                    "Invalid user ID format: " + userId);
        }
    }

    @SneakyThrows
    private void sendLogToKafka(String messageType, String endpoint, Object payload, String appName) {
        Map<String, Object> log = new HashMap<>();
        log.put("message", objectMapper.writeValueAsString(payload));
        log.put("messageType", messageType);
        log.put("dateTime", OffsetDateTime.now());
        log.put("sourceService", "account-service");
        log.put("sourceEndpoint", endpoint);
        log.put("appName", appName);
        kafkaProducerService.sendLog(log);
    }

}