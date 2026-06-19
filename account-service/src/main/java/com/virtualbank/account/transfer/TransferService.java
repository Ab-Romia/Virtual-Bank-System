package com.virtualbank.account.transfer;

import com.virtualbank.account.domain.Account;
import com.virtualbank.account.domain.AccountRepository;
import com.virtualbank.account.domain.ProcessedEvent;
import com.virtualbank.account.domain.ProcessedEventRepository;
import com.virtualbank.common.event.TransferEvent;
import com.virtualbank.common.event.TransferRequested;
import com.virtualbank.common.messaging.Topics;
import com.virtualbank.common.outbox.OutboxAppender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Applies a single intra-bank transfer in one local transaction. Because both
 * accounts live in this database, the debit and credit commit atomically, so a
 * failed transfer leaves no money in flight and needs no compensating step.
 *
 * <p>The outcome (completed or failed) is appended to the transactional outbox in
 * the same transaction, so the orchestrator always learns what happened exactly
 * once the balance change is durable.
 */
@Service
public class TransferService {

    private final AccountRepository accounts;
    private final ProcessedEventRepository processedEvents;
    private final OutboxAppender outbox;
    private final Clock clock;

    public TransferService(AccountRepository accounts, ProcessedEventRepository processedEvents,
                           OutboxAppender outbox, Clock clock) {
        this.accounts = accounts;
        this.processedEvents = processedEvents;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional
    public void apply(TransferRequested command) {
        String transferId = command.transferId();
        if (processedEvents.existsById(transferId)) {
            return;
        }
        processedEvents.save(new ProcessedEvent(transferId, Instant.now(clock)));

        // Lock both accounts in a fixed order (smaller id first). Two opposite-direction
        // transfers between the same pair would otherwise grab the locks in opposite
        // orders and deadlock; the canonical order guarantees they cannot.
        String fromId = command.fromAccountId();
        String toId = command.toAccountId();
        Account from;
        Account to;
        if (fromId.compareTo(toId) <= 0) {
            from = lock(fromId);
            to = lock(toId);
        } else {
            to = lock(toId);
            from = lock(fromId);
        }

        Instant now = Instant.now(clock);
        String reason = validate(command, from, to);
        if (reason != null) {
            outbox.append(transferId, Topics.TRANSFER_EVENTS, transferId,
                    new TransferEvent.TransferFailed(transferId, reason, now));
            return;
        }

        from.debit(command.amount(), now);
        to.credit(command.amount(), now);
        outbox.append(transferId, Topics.TRANSFER_EVENTS, transferId,
                new TransferEvent.TransferCompleted(transferId, now));
    }

    private Account lock(String id) {
        return accounts.findByIdForUpdate(id).orElse(null);
    }

    private String validate(TransferRequested command, Account from, Account to) {
        if (from == null) {
            return "SOURCE_NOT_FOUND";
        }
        if (!from.isActive()) {
            return "SOURCE_NOT_ACTIVE";
        }
        if (!from.getOwnerId().equals(command.initiatorId())) {
            return "SOURCE_NOT_OWNED";
        }
        if (to == null) {
            return "DESTINATION_NOT_FOUND";
        }
        if (!to.isActive()) {
            return "DESTINATION_NOT_ACTIVE";
        }
        if (!from.canCover(command.amount())) {
            return "INSUFFICIENT_FUNDS";
        }
        return null;
    }
}
