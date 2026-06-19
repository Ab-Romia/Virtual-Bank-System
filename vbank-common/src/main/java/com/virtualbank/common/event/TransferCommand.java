package com.virtualbank.common.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A command issued by the transfer orchestrator (transaction-service) to
 * account-service on the {@code transfer.commands} topic. The saga deliberately
 * separates the debit and the credit so the compensating refund path is real;
 * see the design notes in docs/ for why an intra-bank transfer is modelled as a
 * saga rather than a single local transaction.
 *
 * <p>Serialized as JSON with a {@code type} discriminator so one deserializer
 * reconstructs the correct subtype.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TransferCommand.DebitRequested.class, name = "DebitRequested"),
        @JsonSubTypes.Type(value = TransferCommand.CreditRequested.class, name = "CreditRequested"),
        @JsonSubTypes.Type(value = TransferCommand.RefundRequested.class, name = "RefundRequested")
})
public sealed interface TransferCommand
        permits TransferCommand.DebitRequested, TransferCommand.CreditRequested, TransferCommand.RefundRequested {

    /** Unique id of this command, used by account-service for idempotent processing. */
    String eventId();

    /** The transfer this command belongs to; used as the Kafka partition key for ordering. */
    String transferId();

    Instant occurredAt();

    record DebitRequested(String eventId, String transferId, String fromAccountId, String toAccountId,
                          BigDecimal amount, String currency, Instant occurredAt) implements TransferCommand {}

    record CreditRequested(String eventId, String transferId, String fromAccountId, String toAccountId,
                           BigDecimal amount, String currency, Instant occurredAt) implements TransferCommand {}

    record RefundRequested(String eventId, String transferId, String accountId,
                           BigDecimal amount, String currency, Instant occurredAt) implements TransferCommand {}
}
