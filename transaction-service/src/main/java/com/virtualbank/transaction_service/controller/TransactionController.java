package com.virtualbank.transaction_service.controller;

import com.virtualbank.transaction_service.dto.TransferInitiationRequest;
import com.virtualbank.transaction_service.dto.TransferInitiationResponse;
import com.virtualbank.transaction_service.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transfer/initiation")
    public ResponseEntity<TransferInitiationResponse> initiateTransfer(@Valid @RequestBody TransferInitiationRequest request) {
        TransferInitiationResponse response = transactionService.initiateTransfer(request);
        return ResponseEntity.ok(response);
    }
}