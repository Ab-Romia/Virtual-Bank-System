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
public class UserAccountsResponseDto {
    private UUID accountId;
    private UUID userId;
    private String accountNumber;
    private String accountType;
    private BigDecimal balance;
    private String status;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private ZonedDateTime lastTransactionAt;
}