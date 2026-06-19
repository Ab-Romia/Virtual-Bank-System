package com.virtualbank.transaction;

import com.virtualbank.common.event.TransferRequested;
import com.virtualbank.common.messaging.Topics;
import com.virtualbank.common.outbox.OutboxAppender;
import com.virtualbank.common.web.ApiException;
import com.virtualbank.transaction.domain.ProcessedEvent;
import com.virtualbank.transaction.domain.ProcessedEventRepository;
import com.virtualbank.transaction.domain.Transfer;
import com.virtualbank.transaction.domain.TransferRepository;
import com.virtualbank.transaction.web.dto.TransferRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Owns the transfer ledger and orchestrates each transfer. Starting a transfer
 * writes a PENDING row and appends a TransferRequested command in one
 * transaction, so the command is never published without the ledger row
 * committing. Applying an outcome records it once and advances the same row.
 */
@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final TransferRepository transfers;
    private final ProcessedEventRepository processedEvents;
    private final OutboxAppender outbox;
    private final Clock clock;

    public TransferService(TransferRepository transfers, ProcessedEventRepository processedEvents,
                           OutboxAppender outbox, Clock clock) {
        this.transfers = transfers;
        this.processedEvents = processedEvents;
        this.outbox = outbox;
        this.clock = clock;
    }

    /**
     * Starts a transfer for the authenticated initiator. The idempotency key makes
     * a retried request return the original transfer rather than create a second
     * one. Ownership of the source account is enforced downstream by
     * account-service, which checks the initiatorId, so no synchronous call is made
     * from here.
     */
    @Transactional
    public Transfer requestTransfer(String initiatorId, String idempotencyKey, TransferRequest request) {
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("Amount must be greater than zero");
        }
        if (request.fromAccountId().equals(request.toAccountId())) {
            throw ApiException.badRequest("Source and destination accounts must differ");
        }

        return transfers.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> create(initiatorId, idempotencyKey, request));
    }

    private Transfer create(String initiatorId, String idempotencyKey, TransferRequest request) {
        Instant now = Instant.now(clock);
        String transferId = UUID.randomUUID().toString();
        Transfer transfer = new Transfer(transferId, initiatorId, request.fromAccountId(),
                request.toAccountId(), request.amount(), request.currency(), idempotencyKey, now);
        transfers.save(transfer);

        TransferRequested command = new TransferRequested(transferId, initiatorId,
                request.fromAccountId(), request.toAccountId(), request.amount(), request.currency(), now);
        outbox.append(transferId, Topics.TRANSFER_COMMANDS, transferId, command);
        return transfer;
    }

    @Transactional(readOnly = true)
    public Transfer getForInitiator(String transferId, String initiatorId) {
        Transfer transfer = transfers.findById(transferId)
                .orElseThrow(() -> ApiException.notFound("Transfer not found"));
        if (!transfer.getInitiatorId().equals(initiatorId)) {
            throw ApiException.forbidden("Cannot access another user's transfer");
        }
        return transfer;
    }

    @Transactional(readOnly = true)
    public List<Transfer> listForInitiator(String initiatorId) {
        return transfers.findByInitiatorIdOrderByCreatedAtDesc(initiatorId);
    }

    /** Records a successful outcome. Idempotent and a no-op on an already terminal transfer. */
    @Transactional
    public void completeTransfer(String transferId) {
        applyOutcome(transferId, transfer -> transfer.markCompleted(Instant.now(clock)));
    }

    /** Records a failed outcome with its reason. Idempotent and a no-op on an already terminal transfer. */
    @Transactional
    public void failTransfer(String transferId, String reason) {
        applyOutcome(transferId, transfer -> transfer.markFailed(reason, Instant.now(clock)));
    }

    private void applyOutcome(String transferId, java.util.function.Consumer<Transfer> mutation) {
        if (processedEvents.existsById(transferId)) {
            return;
        }
        Transfer transfer = transfers.findById(transferId).orElse(null);
        if (transfer == null) {
            log.warn("Outcome for unknown transfer {}, ignoring", transferId);
            return;
        }
        if (transfer.isTerminal()) {
            return;
        }
        mutation.accept(transfer);
        processedEvents.save(new ProcessedEvent(transferId, Instant.now(clock)));
    }
}
