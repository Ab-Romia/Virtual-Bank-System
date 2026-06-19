package com.virtualbank.audit;

import com.virtualbank.audit.domain.AuditEntryRepository;
import com.virtualbank.audit.web.dto.AuditEntryView;
import com.virtualbank.common.event.TransferEvent;
import com.virtualbank.common.event.TransferRequested;
import com.virtualbank.common.web.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the audit recording and history against a real Postgres. The service is
 * driven directly, so no broker is needed; Kafka auto-configuration is excluded.
 * Postgres comes from Testcontainers so Flyway runs the real schema and
 * ddl-auto=validate confirms the entity matches.
 */
@SpringBootTest(
        properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration")
@Testcontainers
class AuditRecordingTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg17");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditEntryRepository entries;

    private static final String INITIATOR = "user-1";

    @BeforeEach
    void clean() {
        entries.deleteAll();
    }

    @Test
    void requestThenCompletionBuildsTheHistory() {
        String transferId = "transfer-1";
        auditService.recordRequested(new TransferRequested(transferId, INITIATOR, "acc-from", "acc-to",
                new BigDecimal("125.50"), "EGP", Instant.parse("2026-01-01T10:00:00Z")));
        auditService.recordEvent(new TransferEvent.TransferCompleted(transferId,
                Instant.parse("2026-01-01T10:00:05Z")));

        List<AuditEntryView> history = auditService.historyFor(transferId, INITIATOR);

        assertThat(history).extracting(AuditEntryView::eventType).containsExactly("REQUESTED", "COMPLETED");
        assertThat(history.getFirst().amount()).isEqualByComparingTo("125.50");
        assertThat(history.getFirst().currency()).isEqualTo("EGP");
    }

    @Test
    void recordingTheSameRequestTwiceStoresOneRow() {
        String transferId = "transfer-dup";
        TransferRequested command = new TransferRequested(transferId, INITIATOR, "acc-from", "acc-to",
                new BigDecimal("10.00"), "EGP", Instant.parse("2026-01-01T10:00:00Z"));

        auditService.recordRequested(command);
        auditService.recordRequested(command);

        assertThat(entries.count()).isEqualTo(1);
    }

    @Test
    void historyIsReturnedToInitiatorAndRejectedForOthers() {
        String transferId = "transfer-auth";
        auditService.recordRequested(new TransferRequested(transferId, INITIATOR, "acc-from", "acc-to",
                new BigDecimal("20.00"), "EGP", Instant.parse("2026-01-01T10:00:00Z")));
        auditService.recordEvent(new TransferEvent.TransferFailed(transferId, "Insufficient funds",
                Instant.parse("2026-01-01T10:00:05Z")));

        List<AuditEntryView> history = auditService.historyFor(transferId, INITIATOR);
        assertThat(history).hasSize(2);
        assertThat(history.get(1).reason()).isEqualTo("Insufficient funds");

        assertThatThrownBy(() -> auditService.historyFor(transferId, "intruder"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
