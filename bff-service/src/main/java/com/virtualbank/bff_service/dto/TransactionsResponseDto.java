package com.virtualbank.bff_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionsResponseDto {
    private UUID transactionId;
    private BigDecimal amount;
    private UUID fromAccountId;
    private UUID toAccountId;
    private String description;
    private ZonedDateTime timestamp;
    private String status;
}