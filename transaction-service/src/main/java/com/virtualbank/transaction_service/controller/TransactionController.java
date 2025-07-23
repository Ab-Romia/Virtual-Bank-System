package com.virtualbank.transaction_service.controller;

import com.virtualbank.transaction_service.dto.*;
import com.virtualbank.transaction_service.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transactions/transfer/initiation")
    public ResponseEntity<TransferInitiationResponse> initiateTransfer(@Valid @RequestBody TransferInitiationRequest request) {
        TransferInitiationResponse response = transactionService.initiateTransfer(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transactions/transfer/execution")
    public ResponseEntity<TransferExecutionResponse> executeTransfer(@Valid @RequestBody TransferExecutionRequest request){
        TransferExecutionResponse response = transactionService.executeTransfer(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<List<TransactionsResponse>> getTransactionsFromAccount(@PathVariable String accountId){
        UUID Id = UUID.fromString(accountId);
        List<TransactionsResponse> responses = transactionService.getTransactionsFromAccount(Id);
        return ResponseEntity.ok(responses);
    }
}