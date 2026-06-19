package com.virtualbank.transaction;

import com.virtualbank.common.event.TransferRequested;
import com.virtualbank.common.messaging.Topics;
import com.virtualbank.common.outbox.OutboxEntry;
import com.virtualbank.common.outbox.OutboxRepository;
import com.virtualbank.transaction.domain.Transfer;
import com.virtualbank.transaction.domain.TransferRepository;
import com.virtualbank.transaction.domain.TransferStatus;
import com.virtualbank.transaction.web.dto.TransferRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the transfer ledger and orchestration against a real Postgres. The
 * service and the event handlers are driven directly, so no broker is needed;
 * Kafka auto-configuration is excluded. Postgres comes from Testcontainers so
 * Flyway runs the real schema and ddl-auto=validate confirms the entities match.
 *
 * The four behaviours under test: a request creates a PENDING transfer and exactly
 * one TransferRequested command in the outbox; the same Idempotency-Key resolves to
 * the same transfer; outcomes move the transfer to COMPLETED or FAILED; and a
 * replayed outcome never moves a terminal transfer.
 */
@SpringBootTest(
        properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration")
@Testcontainers
class TransferOrchestrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg17");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private TransferService transferService;

    @Autowired
    private TransferRepository transfers;

    @Autowired
    private OutboxRepository outbox;

    private static final String INITIATOR = "user-1";

    @BeforeEach
    void clean() {
        outbox.deleteAll();
        transfers.deleteAll();
    }

    @Test
    void requestCreatesPendingTransferAndOneCommandInOutbox() {
        Transfer transfer = transferService.requestTransfer(INITIATOR, "key-1",
                new TransferRequest("acc-from", "acc-to", new BigDecimal("125.50"), "EGP"));

        assertThat(transfer.getStatus()).isEqualTo(TransferStatus.PENDING);
        assertThat(transfer.getInitiatorId()).isEqualTo(INITIATOR);

        List<OutboxEntry> entries = outbox.findAll();
        assertThat(entries).hasSize(1);
        OutboxEntry entry = entries.getFirst();
        assertThat(entry.getId()).isEqualTo(transfer.getId());
        assertThat(entry.getTopic()).isEqualTo(Topics.TRANSFER_COMMANDS);
        assertThat(entry.getMessageKey()).isEqualTo(transfer.getId());
        assertThat(entry.getType()).isEqualTo(TransferRequested.class.getSimpleName());
        assertThat(entry.getPayload()).contains(transfer.getId(), "acc-from", "acc-to");
    }

    @Test
    void sameIdempotencyKeyReturnsSameTransferAndCreatesOnlyOne() {
        TransferRequest request = new TransferRequest("acc-from", "acc-to", new BigDecimal("10.00"), "EGP");

        Transfer first = transferService.requestTransfer(INITIATOR, "key-dup", request);
        Transfer second = transferService.requestTransfer(INITIATOR, "key-dup", request);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(transfers.count()).isEqualTo(1);
        assertThat(outbox.count()).isEqualTo(1);
    }

    @Test
    void completedAndFailedOutcomesUpdateTheLedger() {
        Transfer completed = transferService.requestTransfer(INITIATOR, "key-ok",
                new TransferRequest("acc-from", "acc-to", new BigDecimal("5.00"), "EGP"));
        transferService.completeTransfer(completed.getId());
        assertThat(reload(completed.getId()).getStatus()).isEqualTo(TransferStatus.COMPLETED);

        Transfer failed = transferService.requestTransfer(INITIATOR, "key-bad",
                new TransferRequest("acc-from", "acc-to", new BigDecimal("7.00"), "EGP"));
        transferService.failTransfer(failed.getId(), "Insufficient funds");
        Transfer reloaded = reload(failed.getId());
        assertThat(reloaded.getStatus()).isEqualTo(TransferStatus.FAILED);
        assertThat(reloaded.getFailureReason()).isEqualTo("Insufficient funds");
    }

    @Test
    void replayedOutcomeDoesNotMoveTerminalTransfer() {
        Transfer transfer = transferService.requestTransfer(INITIATOR, "key-replay",
                new TransferRequest("acc-from", "acc-to", new BigDecimal("9.00"), "EGP"));

        transferService.completeTransfer(transfer.getId());
        // A late or duplicate failure for the same transfer must be ignored.
        transferService.failTransfer(transfer.getId(), "should be ignored");

        Transfer reloaded = reload(transfer.getId());
        assertThat(reloaded.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(reloaded.getFailureReason()).isNull();
    }

    private Transfer reload(String id) {
        return transfers.findById(id).orElseThrow();
    }
}
