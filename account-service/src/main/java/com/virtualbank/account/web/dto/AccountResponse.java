package com.virtualbank.account.web.dto;

import com.virtualbank.account.domain.Account;
import com.virtualbank.account.domain.AccountStatus;
import com.virtualbank.account.domain.AccountType;

import java.math.BigDecimal;
import java.time.Instant;

/** Public view of an account. */
public record AccountResponse(
        String id,
        String ownerId,
        String accountNumber,
        AccountType type,
        BigDecimal balance,
        String currency,
        AccountStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getOwnerId(),
                account.getAccountNumber(),
                account.getType(),
                account.getBalance(),
                account.getCurrency(),
                account.getStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt());
    }
}
