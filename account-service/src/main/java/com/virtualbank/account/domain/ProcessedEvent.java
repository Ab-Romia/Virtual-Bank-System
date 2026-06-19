package com.virtualbank.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * The idempotency inbox. Recording a transferId here, in the same transaction as
 * the balance change and the outbox append, is what makes re-delivery of a command
 * a no-op: a duplicate insert collides on the primary key and the second attempt
 * is skipped.
 */
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    @Column(name = "transfer_id")
    private String transferId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedEvent() {
        // for JPA
    }

    public ProcessedEvent(String transferId, Instant processedAt) {
        this.transferId = transferId;
        this.processedAt = processedAt;
    }

    public String getTransferId() {
        return transferId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
