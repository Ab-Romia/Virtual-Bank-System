package com.virtualbank.common.messaging;

/** Central registry of Kafka topic names so producers and consumers cannot drift. */
public final class Topics {

    private Topics() {
    }

    /** Orchestrator to account-service: DebitRequested, CreditRequested, RefundRequested. */
    public static final String TRANSFER_COMMANDS = "transfer.commands";

    /** Account-service to orchestrator: the outcome of each saga step. */
    public static final String TRANSFER_EVENTS = "transfer.events";

    /** All services to the audit-service. */
    public static final String AUDIT_LOG = "audit.log";

    public static final String TRANSFER_COMMANDS_DLT = "transfer.commands.DLT";
    public static final String TRANSFER_EVENTS_DLT = "transfer.events.DLT";
    public static final String AUDIT_LOG_DLT = "audit.log.DLT";
}
