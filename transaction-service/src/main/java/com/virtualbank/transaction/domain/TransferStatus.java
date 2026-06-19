package com.virtualbank.transaction.domain;

/**
 * The state of a transfer in the ledger, persisted as its name string. A transfer
 * starts PENDING when the command is published and reaches COMPLETED or FAILED once
 * account-service reports the outcome. Both end states are terminal.
 */
public enum TransferStatus {
    PENDING,
    COMPLETED,
    FAILED
}
