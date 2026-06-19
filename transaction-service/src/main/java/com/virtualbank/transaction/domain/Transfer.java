package com.virtualbank.transaction.domain;

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
 * A single intra-bank transfer and its place in the ledger. The id is the
 * transferId used as the Kafka partition key, so every command and event for one
 * transfer stays ordered. The idempotency key is unique so a retried request
 * resolves to the same transfer rather than a second one. The optimistic
 * {@code version} guards the row that both the REST thread and the event listener
 * can touch.
 */
@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    private String id;

    @Column(name = "initiator_id", nullable = false)
    private String initiatorId;

    @Column(name = "from_account_id", nullable = false)
    private String fromAccountId;

    @Column(name = "to_account_id", nullable = false)
    private String toAccountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected Transfer() {
        // for JPA
    }

    public Transfer(String id, String initiatorId, String fromAccountId, String toAccountId,
                    BigDecimal amount, String currency, String idempotencyKey, Instant now) {
        this.id = id;
        this.initiatorId = initiatorId;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
        this.status = TransferStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void markCompleted(Instant when) {
        this.status = TransferStatus.COMPLETED;
        this.updatedAt = when;
    }

    public void markFailed(String reason, Instant when) {
        this.status = TransferStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = when;
    }

    /** A terminal transfer must not be moved by a late or duplicate event. */
    public boolean isTerminal() {
        return status == TransferStatus.COMPLETED || status == TransferStatus.FAILED;
    }

    public String getId() {
        return id;
    }

    public String getInitiatorId() {
        return initiatorId;
    }

    public String getFromAccountId() {
        return fromAccountId;
    }

    public String getToAccountId() {
        return toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
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
