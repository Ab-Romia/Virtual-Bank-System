package com.virtualbank.account.dto;

import com.virtualbank.account.model.Account;
import com.virtualbank.account.model.AccountType;
import com.virtualbank.account.model.Status;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAccountsResponse {
    private UUID accountId;
    private String accountNumber;
    private BigDecimal balance;
    @Enumerated(EnumType.STRING)
    private AccountType accountType;
    @Enumerated(EnumType.STRING)
    private Status status;

    public UserAccountsResponse(Account account) {
        this.accountId = account.getAccountId();
        this.accountNumber = account.getAccountNumber();
        this.balance = account.getBalance();
        this.accountType = account.getAccountType();
        this.status = account.getStatus();
    }
}
