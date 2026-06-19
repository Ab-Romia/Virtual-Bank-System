package com.virtualbank.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Account-service owns bank accounts and applies money transfers. It consumes
 * commands on transfer.commands, moves the money in one local transaction, and
 * reports each outcome through the transactional outbox so an event is never lost
 * or published without its balance change committing.
 *
 * <p>The scans are widened to include the shared outbox package so vbank-common's
 * OutboxEntry and OutboxRepository are picked up here. Everything else (security,
 * exception handling, the outbox relay) arrives through vbank-common
 * auto-configuration.
 */
@SpringBootApplication
@EntityScan({"com.virtualbank.account", "com.virtualbank.common.outbox"})
@EnableJpaRepositories({"com.virtualbank.account", "com.virtualbank.common.outbox"})
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
}
