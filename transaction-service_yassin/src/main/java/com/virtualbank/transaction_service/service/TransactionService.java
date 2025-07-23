package com.virtualbank.transaction_service.service;

import com.virtualbank.transaction_service.dto.*;
import com.virtualbank.transaction_service.entity.Transaction;
import com.virtualbank.transaction_service.entity.TransactionStatus;
import com.virtualbank.transaction_service.entity.TransactionType;
import com.virtualbank.transaction_service.exception.AccNotFoundException;
import com.virtualbank.transaction_service.exception.InsufficientFundsException;
import com.virtualbank.transaction_service.exception.TransactionNotFoundException;
import com.virtualbank.transaction_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository, RestTemplate restTemplate) {
        this.transactionRepository = transactionRepository;
        this.restTemplate = restTemplate;
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

    @Transactional
    public TransferExecutionResponse executeTransfer(TransferExecutionRequest request){
        UUID id = request.getTransactionId();
        Optional<Transaction> transactionOptional = transactionRepository.findByTransactionId(id);
        if(transactionOptional.isEmpty()){
            throw new TransactionNotFoundException("Transaction was not initiated");
        }
        Transaction transaction = transactionOptional.get();
        UUID fromId = transaction.getFromAccountId();
        UUID toId = transaction.getToAccountId();
        String fromIdStr = fromId.toString();
        String toIdStr = toId.toString();

        //get account objects
        AccountDto fromAccount = restTemplate.getForObject("http://localhost:8082/accounts/" + fromIdStr, AccountDto.class);
        AccountDto toAccount = restTemplate.getForObject("http://localhost:8082/accounts/" + toIdStr, AccountDto.class);

        //Check for Accounts Existing
        if(fromAccount == null || toAccount == null){
            throw new AccNotFoundException("Invalid account or insufficient funds");
        }

        //Check for Insufficient Funds
        if(fromAccount.getBalance().compareTo(transaction.getAmount()) < 0 ){
            throw new InsufficientFundsException("Invalid account or insufficient funds");
        }

        //Implementation still in progress
        return null;
    }
}