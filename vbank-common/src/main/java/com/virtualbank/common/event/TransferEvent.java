package com.virtualbank.common.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * An event emitted by account-service to the orchestrator on the
 * {@code transfer.events} topic, reporting the outcome of each saga step.
 * Serialized as JSON with a {@code type} discriminator.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TransferEvent.SourceDebited.class, name = "SourceDebited"),
        @JsonSubTypes.Type(value = TransferEvent.DebitFailed.class, name = "DebitFailed"),
        @JsonSubTypes.Type(value = TransferEvent.DestinationCredited.class, name = "DestinationCredited"),
        @JsonSubTypes.Type(value = TransferEvent.CreditFailed.class, name = "CreditFailed"),
        @JsonSubTypes.Type(value = TransferEvent.SourceRefunded.class, name = "SourceRefunded")
})
public sealed interface TransferEvent
        permits TransferEvent.SourceDebited, TransferEvent.DebitFailed, TransferEvent.DestinationCredited,
                TransferEvent.CreditFailed, TransferEvent.SourceRefunded {

    String eventId();

    String transferId();

    Instant occurredAt();

    record SourceDebited(String eventId, String transferId, String fromAccountId, String toAccountId,
                         BigDecimal amount, String currency, Instant occurredAt) implements TransferEvent {}

    record DebitFailed(String eventId, String transferId, String fromAccountId, String reason,
                       Instant occurredAt) implements TransferEvent {}

    record DestinationCredited(String eventId, String transferId, String toAccountId,
                               BigDecimal amount, String currency, Instant occurredAt) implements TransferEvent {}

    record CreditFailed(String eventId, String transferId, String toAccountId, String reason,
                        Instant occurredAt) implements TransferEvent {}

    record SourceRefunded(String eventId, String transferId, String fromAccountId,
                          BigDecimal amount, String currency, Instant occurredAt) implements TransferEvent {}
}
