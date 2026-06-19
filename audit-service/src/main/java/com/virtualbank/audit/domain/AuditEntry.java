package com.virtualbank.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One immutable record of something that happened to a transfer. The REQUESTED
 * entry carries the transfer's parties and amount; COMPLETED and FAILED carry
 * the outcome, with FAILED also keeping the reason. The unique constraint on
 * (transferId, eventType) makes a redelivered message resolve to the same row
 * rather than a duplicate. Entries are never updated once written.
 */
@Entity
@Table(name = "audit_entries",
        uniqueConstraints = @UniqueConstraint(name = "uq_audit_transfer_event",
                columnNames = {"transfer_id", "event_type"}))
public class AuditEntry {

    @Id
    private String id;

    @Column(name = "transfer_id", nullable = false)
    private String transferId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private AuditEventType eventType;

    @Column(name = "initiator_id")
    private String initiatorId;

    @Column(name = "from_account_id")
    private String fromAccountId;

    @Column(name = "to_account_id")
    private String toAccountId;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Column
    private String currency;

    @Column
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected AuditEntry() {
        // for JPA
    }

    private AuditEntry(String id, String transferId, AuditEventType eventType, Instant occurredAt,
                       Instant recordedAt) {
        this.id = id;
        this.transferId = transferId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.recordedAt = recordedAt;
    }

    public static AuditEntry requested(String id, String transferId, String initiatorId, String fromAccountId,
                                       String toAccountId, BigDecimal amount, String currency,
                                       Instant occurredAt, Instant recordedAt) {
        AuditEntry entry = new AuditEntry(id, transferId, AuditEventType.REQUESTED, occurredAt, recordedAt);
        entry.initiatorId = initiatorId;
        entry.fromAccountId = fromAccountId;
        entry.toAccountId = toAccountId;
        entry.amount = amount;
        entry.currency = currency;
        return entry;
    }

    public static AuditEntry completed(String id, String transferId, Instant occurredAt, Instant recordedAt) {
        return new AuditEntry(id, transferId, AuditEventType.COMPLETED, occurredAt, recordedAt);
    }

    public static AuditEntry failed(String id, String transferId, String reason, Instant occurredAt,
                                    Instant recordedAt) {
        AuditEntry entry = new AuditEntry(id, transferId, AuditEventType.FAILED, occurredAt, recordedAt);
        entry.reason = reason;
        return entry;
    }

    public String getId() {
        return id;
    }

    public String getTransferId() {
        return transferId;
    }

    public AuditEventType getEventType() {
        return eventType;
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

    public String getReason() {
        return reason;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
