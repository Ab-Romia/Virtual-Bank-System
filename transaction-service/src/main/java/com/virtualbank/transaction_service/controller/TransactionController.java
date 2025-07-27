package com.virtualbank.transaction_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualbank.transaction_service.dto.*;
import com.virtualbank.transaction_service.service.KafkaProducerService;
import com.virtualbank.transaction_service.service.TransactionService;
import jakarta.validation.Valid;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class TransactionController {

    private final TransactionService transactionService;
    private final KafkaProducerService kafkaProducerService;
    private final ObjectMapper objectMapper;

    @Autowired
    public TransactionController(TransactionService transactionService,
                                 KafkaProducerService kafkaProducerService,
                                 ObjectMapper objectMapper) {
        this.transactionService = transactionService;
        this.kafkaProducerService = kafkaProducerService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/transactions/transfer/initiation")
    public ResponseEntity<TransferInitiationResponse> initiateTransfer(
            @Valid @RequestBody TransferInitiationRequest request,
            @RequestHeader(name = "APP-NAME", required = false, defaultValue = "UNKNOWN") String appName) {

        String endpoint = "/transactions/transfer/initiation";
        sendLogToKafka("Request", endpoint, request, appName);

        TransferInitiationResponse response = transactionService.initiateTransfer(request);

        sendLogToKafka("Response", endpoint, response, appName);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transactions/transfer/execution")
    public ResponseEntity<TransferExecutionResponse> executeTransfer(
            @Valid @RequestBody TransferExecutionRequest request,
            @RequestHeader(name = "APP-NAME", required = false, defaultValue = "UNKNOWN") String appName) {

        String endpoint = "/transactions/transfer/execution";
        sendLogToKafka("Request", endpoint, request, appName);

        TransferExecutionResponse response = transactionService.executeTransfer(request);

        sendLogToKafka("Response", endpoint, response, appName);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<List<TransactionsResponse>> getTransactionsFromAccount(
            @PathVariable String accountId,
            @RequestHeader(name = "APP-NAME", required = false, defaultValue = "UNKNOWN") String appName) {

        String endpoint = "/accounts/" + accountId + "/transactions";
        sendLogToKafka("Request", endpoint, "Get transactions for accountId: " + accountId, appName);

        UUID id = UUID.fromString(accountId);
        List<TransactionsResponse> responses = transactionService.getTransactionsFromAccount(id);

        sendLogToKafka("Response", endpoint, responses, appName);
        return ResponseEntity.ok(responses);
    }

    @SneakyThrows
    private void sendLogToKafka(String messageType, String endpoint, Object payload, String appName) {
        Map<String, Object> log = new HashMap<>();
        log.put("message", objectMapper.writeValueAsString(payload));
        log.put("messageType", messageType);
        log.put("dateTime", OffsetDateTime.now());
        log.put("sourceService", "transaction-service");
        log.put("sourceEndpoint", endpoint);
        log.put("appName", appName);
        kafkaProducerService.sendLog(log);
    }
}