package com.virtualbank.transaction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * The events-consumer idempotency inbox. A transfer has exactly one terminal
 * outcome, so the transferId is the natural key: recording it in the same
 * transaction as the ledger update makes re-delivery of that outcome a no-op,
 * because the listener skips any transferId already present here.
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
