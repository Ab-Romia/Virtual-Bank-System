package com.virtualbank.audit.web.dto;

import com.virtualbank.audit.domain.AuditEntry;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One step of a transfer's audit history. Amount and currency are present only
 * on the REQUESTED entry and reason only on a FAILED one, so the unused fields
 * are null for the other event types.
 */
public record AuditEntryView(
        String eventType,
        BigDecimal amount,
        String currency,
        String reason,
        Instant occurredAt) {

    public static AuditEntryView of(AuditEntry entry) {
        return new AuditEntryView(
                entry.getEventType().name(),
                entry.getAmount(),
                entry.getCurrency(),
                entry.getReason(),
                entry.getOccurredAt());
    }
}
