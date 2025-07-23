package com.virtualbank.transaction_service.dto;

import com.virtualbank.transaction_service.entity.AccountType;
import com.virtualbank.transaction_service.entity.Status;
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
public class AccountDto {

    private UUID accountId;

    private UUID userId;

    private String accountNumber;

    private AccountType accountType;

    private BigDecimal balance;

    private Status status;

    private ZonedDateTime createdAt;

    private ZonedDateTime updatedAt;

    private ZonedDateTime lastTransactionAt;
}
