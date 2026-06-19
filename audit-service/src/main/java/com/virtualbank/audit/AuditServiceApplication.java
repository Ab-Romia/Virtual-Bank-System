package com.virtualbank.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Event-sourced audit log. It subscribes to the transfer command and event
 * streams and records each message as an immutable AuditEntry, building a
 * queryable history of every transfer. Recording is idempotent so a redelivered
 * message is stored once. Security, exception handling, and the shared events
 * arrive through vbank-common; this service has no outbox of its own.
 */
@SpringBootApplication
public class AuditServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
    }
}
