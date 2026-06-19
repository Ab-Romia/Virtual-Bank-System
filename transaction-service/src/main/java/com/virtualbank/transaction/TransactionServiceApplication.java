package com.virtualbank.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Transfer orchestrator. It owns the transfer ledger and drives an intra-bank
 * transfer in two steps: publish a TransferRequested command asking
 * account-service to move the money, then record the outcome that comes back on
 * transfer.events. Commands leave through vbank-common's transactional outbox so
 * a ledger row and the message that follows from it commit together; outcomes are
 * applied idempotently so a duplicate or late event never moves a finished transfer.
 *
 * <p>The entity and repository scans include com.virtualbank.common.outbox so the
 * shared OutboxEntry and OutboxRepository are managed inside this service's
 * persistence context. Everything else (security, exception handling, the outbox
 * relay) arrives through vbank-common auto-configuration.
 */
@SpringBootApplication
@EntityScan({"com.virtualbank.transaction", "com.virtualbank.common.outbox"})
@EnableJpaRepositories({"com.virtualbank.transaction", "com.virtualbank.common.outbox"})
public class TransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
}
