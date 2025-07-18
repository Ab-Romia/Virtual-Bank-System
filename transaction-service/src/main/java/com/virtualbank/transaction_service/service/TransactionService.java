package com.virtualbank.transaction_service.service;

import com.virtualbank.transaction_service.dto.TransferInitiationRequest;
import com.virtualbank.transaction_service.dto.TransferInitiationResponse;
import com.virtualbank.transaction_service.entity.Transaction;
import com.virtualbank.transaction_service.entity.TransactionStatus;
import com.virtualbank.transaction_service.entity.TransactionType;
import com.virtualbank.transaction_service.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public TransferInitiationResponse initiateTransfer(TransferInitiationRequest request) {
        Transaction transaction = Transaction.builder()
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .amount(request.getAmount())
                .description(request.getDescription())
                .status(TransactionStatus.INITIATED)
                .transactionType(TransactionType.TRANSFER)
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        return TransferInitiationResponse.builder()
                .transactionId(savedTransaction.getTransactionId())
                .status(savedTransaction.getStatus().name())
                .timestamp(savedTransaction.getCreatedAt())
                .build();
    }
}