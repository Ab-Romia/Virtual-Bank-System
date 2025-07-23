package com.virtualbank.transaction_service.service;

import com.virtualbank.transaction_service.dto.*;
import com.virtualbank.transaction_service.entity.Transaction;
import com.virtualbank.transaction_service.dto.TransferRequest;
import com.virtualbank.transaction_service.entity.TransactionStatus;
import com.virtualbank.transaction_service.entity.TransactionType;
import com.virtualbank.transaction_service.exception.AccountNotFoundException;
import com.virtualbank.transaction_service.exception.InsufficientFundsException;
import com.virtualbank.transaction_service.exception.TransactionNotFoundException;
import com.virtualbank.transaction_service.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate;
    private final String accountServiceUrl;
    @Autowired
    public TransactionService(TransactionRepository transactionRepository,
                              RestTemplate restTemplate,
                              @Value("${account.service.url}") String accountServiceUrl) {
        this.transactionRepository = transactionRepository;
        this.restTemplate = restTemplate;
        this.accountServiceUrl = accountServiceUrl;
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
    public TransferExecutionResponse executeTransfer(TransferExecutionRequest request) {
        UUID id = request.getTransactionId();
        Optional<Transaction> transactionOptional = transactionRepository.findByTransactionId(id);

        if (transactionOptional.isEmpty()) {
            throw new TransactionNotFoundException("Transaction with ID " + id + " was not initiated");
        }

        Transaction transaction = transactionOptional.get();

        // If transaction is already processed, don't process it again
        if (transaction.getStatus() != TransactionStatus.INITIATED) {
            return TransferExecutionResponse.builder()
                    .transactionId(transaction.getTransactionId())
                    .timestamp(transaction.getCreatedAt())
                    .status(transaction.getStatus())
                    .build();
        }

        UUID fromId = transaction.getFromAccountId();
        UUID toId = transaction.getToAccountId();

        try {
            // Get account objects
            AccountDto fromAccount = restTemplate.getForObject(
                    accountServiceUrl + "/accounts/" + fromId, AccountDto.class);
            AccountDto toAccount = restTemplate.getForObject(
                    accountServiceUrl + "/accounts/" + toId, AccountDto.class);

            // Check for Accounts Existing
            if (fromAccount == null || toAccount == null) {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setFailureReason("One or more accounts not found");
                transactionRepository.save(transaction);
                throw new AccountNotFoundException("One or more accounts not found");
            }

            // Check for Inactive Accounts
            if ("INACTIVE".equals(fromAccount.getStatus()) || "INACTIVE".equals(toAccount.getStatus())) {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setFailureReason("One or more accounts are not active");
                transactionRepository.save(transaction);
                throw new AccountNotFoundException("One or more accounts are not active");
            }

            // Check for Insufficient Funds
            if (fromAccount.getBalance().compareTo(transaction.getAmount()) < 0) {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setFailureReason("Insufficient funds");
                transactionRepository.save(transaction);
                throw new InsufficientFundsException("Insufficient funds in source account");
            }

            // Perform the transfer
            TransferRequest transferRequest = new TransferRequest();
            transferRequest.setFromAccountId(fromId);
            transferRequest.setToAccountId(toId);
            transferRequest.setAmount(transaction.getAmount());

            try {
                restTemplate.put(accountServiceUrl + "/accounts/transfer",
                        transferRequest, Object.class);

                transaction.setStatus(TransactionStatus.SUCCESS);
                Transaction savedTransaction = transactionRepository.save(transaction);

                return TransferExecutionResponse.builder()
                        .transactionId(savedTransaction.getTransactionId())
                        .timestamp(savedTransaction.getCreatedAt())
                        .status(savedTransaction.getStatus())
                        .build();
            } catch (Exception e) {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setFailureReason("Error during transfer: " + e.getMessage());
                transactionRepository.save(transaction);
                throw new RuntimeException("Error executing transfer", e);
            }
        } catch (AccountNotFoundException | InsufficientFundsException e) {
            throw e;
        } catch (Exception e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason("Error during validation: " + e.getMessage());
            transactionRepository.save(transaction);
            throw new RuntimeException("Error validating accounts", e);
        }
    }

    @Transactional
    public List<TransactionsResponse> getTransactionsFromAccount(UUID accountId) {
        List<Transaction> transactions = transactionRepository.findByFromAccountIdOrToAccountIdAndStatus(
                accountId, accountId, TransactionStatus.SUCCESS);

        if(transactions.isEmpty()) {
            throw new TransactionNotFoundException("No successful transactions found");
        }

        List<TransactionsResponse> response = transactions.stream()
                .map(t -> new TransactionsResponse(t, accountId))
                .collect(Collectors.toList());

        return response;
    }
}