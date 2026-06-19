package com.virtualbank.common.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published by the transfer orchestrator (transaction-service) on the
 * transfer.commands topic, asking account-service to move the money. The
 * transferId is the Kafka key, so a transfer's messages keep their order, and
 * the idempotency key account-service uses to apply each transfer exactly once.
 * initiatorId is the authenticated user who started the transfer; account-service
 * confirms that user owns the source account before debiting it.
 */
public record TransferRequested(
        String transferId,
        String initiatorId,
        String fromAccountId,
        String toAccountId,
        BigDecimal amount,
        String currency,
        Instant occurredAt
) {
}
