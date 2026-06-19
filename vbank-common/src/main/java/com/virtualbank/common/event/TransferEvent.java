package com.virtualbank.common.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;

/**
 * Published by account-service on the transfer.events topic once it has applied
 * or rejected a transfer. The orchestrator consumes it to finalize the ledger.
 * A transfer succeeds or fails as a whole, so there are exactly two outcomes,
 * carried polymorphically by the type discriminator.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TransferEvent.TransferCompleted.class, name = "TransferCompleted"),
        @JsonSubTypes.Type(value = TransferEvent.TransferFailed.class, name = "TransferFailed")
})
public sealed interface TransferEvent permits TransferEvent.TransferCompleted, TransferEvent.TransferFailed {

    String transferId();

    Instant occurredAt();

    record TransferCompleted(String transferId, Instant occurredAt) implements TransferEvent {}

    record TransferFailed(String transferId, String reason, Instant occurredAt) implements TransferEvent {}
}
