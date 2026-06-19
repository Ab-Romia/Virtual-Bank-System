package com.virtualbank.transaction.web.dto;

import com.virtualbank.transaction.domain.Transfer;

import java.math.BigDecimal;
import java.time.Instant;

/** The read model returned by the GET endpoints. */
public record TransferView(
        String transferId,
        String fromAccountId,
        String toAccountId,
        BigDecimal amount,
        String currency,
        String status,
        String failureReason,
        Instant createdAt,
        Instant updatedAt) {

    public static TransferView of(Transfer transfer) {
        return new TransferView(
                transfer.getId(),
                transfer.getFromAccountId(),
                transfer.getToAccountId(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getStatus().name(),
                transfer.getFailureReason(),
                transfer.getCreatedAt(),
                transfer.getUpdatedAt());
    }
}
