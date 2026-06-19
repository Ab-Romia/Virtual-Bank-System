package com.virtualbank.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A bank account and the single source of truth for its balance. The id is a UUID
 * assigned by the application; ownerId is the JWT subject of the owner, which is
 * how ownership is enforced without trusting any request input.
 *
 * <p>Balances are moved by loading both accounts under a pessimistic write lock
 * (see TransferService), so concurrent debits serialize and cannot double-spend.
 * The {@code @Version} column guards the create and freeze paths that load and
 * save outside that lock.
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private String id;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected Account() {
        // for JPA
    }

    public Account(String id, String ownerId, String accountNumber, AccountType type,
                   BigDecimal balance, String currency, AccountStatus status,
                   Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.accountNumber = accountNumber;
        this.type = type;
        this.balance = balance;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public boolean canCover(BigDecimal amount) {
        return balance.compareTo(amount) >= 0;
    }

    public void debit(BigDecimal amount, Instant when) {
        this.balance = this.balance.subtract(amount);
        this.updatedAt = when;
    }

    public void credit(BigDecimal amount, Instant when) {
        this.balance = this.balance.add(amount);
        this.updatedAt = when;
    }

    public void freeze(Instant when) {
        this.status = AccountStatus.FROZEN;
        this.updatedAt = when;
    }

    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public AccountType getType() {
        return type;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
