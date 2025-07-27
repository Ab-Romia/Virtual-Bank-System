package com.virtualbank.transaction_service.dto;

import com.virtualbank.transaction_service.entity.Transaction;
import com.virtualbank.transaction_service.entity.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionsResponse {
    private UUID transactionId;
    private UUID accountId;
    private BigDecimal amount;
    private String description;
    private ZonedDateTime timestamp;
    private String status;
    public TransactionsResponse(Transaction transaction, UUID accountId) {
        this.transactionId = transaction.getTransactionId();
        this.accountId = accountId;
        this.amount = transaction.getAmount();
        this.description = transaction.getDescription();
        this.timestamp = transaction.getCreatedAt();
        this.status = transaction.getStatus().name();
    }
}