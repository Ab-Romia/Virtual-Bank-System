package com.virtualbank.account;

import com.virtualbank.account.domain.Account;
import com.virtualbank.account.domain.AccountRepository;
import com.virtualbank.account.domain.AccountStatus;
import com.virtualbank.account.web.dto.CreateAccountRequest;
import com.virtualbank.common.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Create, read, and freeze accounts. Ownership is always the caller's token
 * subject; reads and the freeze enforce that the caller owns the account.
 */
@Service
public class AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accounts;
    private final Clock clock;

    public AccountService(AccountRepository accounts, Clock clock) {
        this.accounts = accounts;
        this.clock = clock;
    }

    @Transactional
    public Account create(String ownerId, CreateAccountRequest request) {
        Instant now = Instant.now(clock);
        Account account = new Account(
                UUID.randomUUID().toString(),
                ownerId,
                generateAccountNumber(),
                request.type(),
                BigDecimal.ZERO,
                request.currency(),
                AccountStatus.ACTIVE,
                now,
                now);
        return accounts.save(account);
    }

    @Transactional(readOnly = true)
    public List<Account> listOwnedBy(String ownerId) {
        return accounts.findByOwnerId(ownerId);
    }

    @Transactional(readOnly = true)
    public Account getOwned(String accountId, String ownerId) {
        return requireOwned(accountId, ownerId);
    }

    @Transactional
    public Account freeze(String accountId, String ownerId) {
        Account account = requireOwned(accountId, ownerId);
        account.freeze(Instant.now(clock));
        return account;
    }

    private Account requireOwned(String accountId, String ownerId) {
        Account account = accounts.findById(accountId)
                .orElseThrow(() -> ApiException.notFound("Account not found"));
        if (!account.getOwnerId().equals(ownerId)) {
            throw ApiException.forbidden("Cannot access another user's account");
        }
        return account;
    }

    private String generateAccountNumber() {
        String number;
        do {
            number = String.format("%012d", Math.abs(RANDOM.nextLong()) % 1_000_000_000_000L);
        } while (accounts.existsByAccountNumber(number));
        return number;
    }
}
