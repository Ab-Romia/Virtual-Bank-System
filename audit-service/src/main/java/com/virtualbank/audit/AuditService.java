package com.virtualbank.audit;

import com.virtualbank.audit.domain.AuditEntry;
import com.virtualbank.audit.domain.AuditEntryRepository;
import com.virtualbank.audit.domain.AuditEventType;
import com.virtualbank.audit.web.dto.AuditEntryView;
import com.virtualbank.common.event.TransferEvent;
import com.virtualbank.common.event.TransferRequested;
import com.virtualbank.common.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Records the transfer stream into the audit log and serves the history back.
 * Recording is idempotent: a single-partition, single-group consumer means no
 * concurrency, so an exists-check before insert keeps a redelivered message to
 * one row, with the unique constraint on (transferId, eventType) as the backstop.
 */
@Service
public class AuditService {

    private final AuditEntryRepository entries;
    private final Clock clock;

    public AuditService(AuditEntryRepository entries, Clock clock) {
        this.entries = entries;
        this.clock = clock;
    }

    @Transactional
    public void recordRequested(TransferRequested command) {
        if (alreadyRecorded(command.transferId(), AuditEventType.REQUESTED)) {
            return;
        }
        entries.save(AuditEntry.requested(newId(), command.transferId(), command.initiatorId(),
                command.fromAccountId(), command.toAccountId(), command.amount(), command.currency(),
                command.occurredAt(), now()));
    }

    @Transactional
    public void recordEvent(TransferEvent event) {
        switch (event) {
            case TransferEvent.TransferCompleted completed -> recordCompleted(completed);
            case TransferEvent.TransferFailed failed -> recordFailed(failed);
        }
    }

    private void recordCompleted(TransferEvent.TransferCompleted event) {
        if (alreadyRecorded(event.transferId(), AuditEventType.COMPLETED)) {
            return;
        }
        entries.save(AuditEntry.completed(newId(), event.transferId(), event.occurredAt(), now()));
    }

    private void recordFailed(TransferEvent.TransferFailed event) {
        if (alreadyRecorded(event.transferId(), AuditEventType.FAILED)) {
            return;
        }
        entries.save(AuditEntry.failed(newId(), event.transferId(), event.reason(), event.occurredAt(), now()));
    }

    /**
     * Returns the ordered history of a transfer to the initiator recorded on its
     * REQUESTED entry. A transfer with no entries is a 404; a caller who is not the
     * initiator is forbidden, which keeps one user from reading another's history.
     */
    @Transactional(readOnly = true)
    public List<AuditEntryView> historyFor(String transferId, String callerId) {
        List<AuditEntry> history = entries.findByTransferIdOrderByOccurredAt(transferId);
        if (history.isEmpty()) {
            throw ApiException.notFound("No audit history for transfer");
        }
        if (!callerId.equals(initiatorOf(history))) {
            throw ApiException.forbidden("Cannot access another user's audit history");
        }
        return history.stream().map(AuditEntryView::of).toList();
    }

    private String initiatorOf(List<AuditEntry> history) {
        return history.stream()
                .filter(entry -> entry.getEventType() == AuditEventType.REQUESTED)
                .map(AuditEntry::getInitiatorId)
                .findFirst()
                .orElse(null);
    }

    private boolean alreadyRecorded(String transferId, AuditEventType eventType) {
        return entries.existsByTransferIdAndEventType(transferId, eventType);
    }

    private Instant now() {
        return Instant.now(clock);
    }

    private String newId() {
        return UUID.randomUUID().toString();
    }
}
