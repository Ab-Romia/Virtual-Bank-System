package com.virtualbank.account.transfer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualbank.account.domain.Account;
import com.virtualbank.account.domain.AccountRepository;
import com.virtualbank.account.domain.AccountStatus;
import com.virtualbank.account.domain.AccountType;
import com.virtualbank.common.event.TransferRequested;
import com.virtualbank.common.messaging.Topics;
import com.virtualbank.common.outbox.OutboxEntry;
import com.virtualbank.common.outbox.OutboxRepository;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the transfer applies money correctly, idempotently, and race-safely.
 * Postgres comes from Testcontainers so Flyway runs the real schema and
 * ddl-auto=validate confirms the entities match. Kafka autoconfiguration is
 * excluded: the test drives TransferService directly and reads the outbox table,
 * so no broker is needed. With Kafka gone the OutboxRelay is not created, so
 * appended events stay in the outbox for the assertions.
 */
@SpringBootTest(
        properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration")
@Testcontainers
class TransferServiceTest {

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
    private AccountRepository accounts;

    @Autowired
    private OutboxRepository outbox;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String OWNER = "owner-1";
    private static final AtomicLong ACCOUNT_NUMBERS = new AtomicLong();

    @BeforeEach
    void clean() {
        outbox.deleteAll();
        accounts.deleteAll();
    }

    @Test
    void validTransferMovesMoneyAndCompletes() {
        Account source = seedAccount(OWNER, new BigDecimal("100.00"));
        Account destination = seedAccount(OWNER, new BigDecimal("40.00"));

        transferService.apply(command(source, destination, new BigDecimal("30.00")));

        assertThat(balanceOf(source)).isEqualByComparingTo("70.00");
        assertThat(balanceOf(destination)).isEqualByComparingTo("70.00");

        List<OutboxEntry> events = outbox.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getTopic()).isEqualTo(Topics.TRANSFER_EVENTS);
        assertThat(events.getFirst().getType()).isEqualTo("TransferCompleted");
    }

    @Test
    void insufficientFundsFailsWithoutMovingMoney() {
        Account source = seedAccount(OWNER, new BigDecimal("10.00"));
        Account destination = seedAccount(OWNER, new BigDecimal("0.00"));

        transferService.apply(command(source, destination, new BigDecimal("50.00")));

        assertThat(balanceOf(source)).isEqualByComparingTo("10.00");
        assertThat(balanceOf(destination)).isEqualByComparingTo("0.00");
        assertFailedWith("INSUFFICIENT_FUNDS");
    }

    @Test
    void wrongOwnerFailsAsNotOwned() {
        Account source = seedAccount("someone-else", new BigDecimal("100.00"));
        Account destination = seedAccount(OWNER, new BigDecimal("0.00"));

        // The initiator (OWNER) does not own the source account.
        transferService.apply(command(source, destination, new BigDecimal("10.00")));

        assertThat(balanceOf(source)).isEqualByComparingTo("100.00");
        assertThat(balanceOf(destination)).isEqualByComparingTo("0.00");
        assertFailedWith("SOURCE_NOT_OWNED");
    }

    @Test
    void sameTransferIdMovesMoneyOnce() {
        Account source = seedAccount(OWNER, new BigDecimal("100.00"));
        Account destination = seedAccount(OWNER, new BigDecimal("0.00"));

        TransferRequested command = command(source, destination, new BigDecimal("25.00"));
        transferService.apply(command);
        transferService.apply(command);

        assertThat(balanceOf(source)).isEqualByComparingTo("75.00");
        assertThat(balanceOf(destination)).isEqualByComparingTo("25.00");
        assertThat(outbox.findAll()).hasSize(1);
    }

    @Test
    void concurrentTransfersNeverDoubleSpend() throws InterruptedException {
        Account source = seedAccount(OWNER, new BigDecimal("100.00"));
        Account destination = seedAccount(OWNER, new BigDecimal("0.00"));

        int transfers = 20;
        BigDecimal amount = new BigDecimal("10.00");
        ExecutorService pool = Executors.newFixedThreadPool(transfers);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(transfers);

        for (int i = 0; i < transfers; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    transferService.apply(command(source, destination, amount));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        // The source can fund exactly 10 of the 20 transfers; the lock must
        // serialize them so it lands on 0 and never goes negative.
        assertThat(balanceOf(source)).isEqualByComparingTo("0.00");
        assertThat(balanceOf(destination)).isEqualByComparingTo("100.00");

        int completed = 0;
        int insufficient = 0;
        for (OutboxEntry entry : outbox.findAll()) {
            if ("TransferCompleted".equals(entry.getType())) {
                completed++;
            } else if ("INSUFFICIENT_FUNDS".equals(reasonOf(entry))) {
                insufficient++;
            }
        }
        assertThat(completed).isEqualTo(10);
        assertThat(insufficient).isEqualTo(10);
    }

    private Account seedAccount(String ownerId, BigDecimal balance) {
        Instant now = Instant.now();
        Account account = new Account(
                UUID.randomUUID().toString(),
                ownerId,
                Long.toString(ACCOUNT_NUMBERS.incrementAndGet()),
                AccountType.CHECKING,
                balance,
                "USD",
                AccountStatus.ACTIVE,
                now,
                now);
        return accounts.save(account);
    }

    private TransferRequested command(Account from, Account to, BigDecimal amount) {
        return new TransferRequested(
                UUID.randomUUID().toString(),
                OWNER,
                from.getId(),
                to.getId(),
                amount,
                "USD",
                Instant.now());
    }

    private BigDecimal balanceOf(Account account) {
        return accounts.findById(account.getId()).orElseThrow().getBalance();
    }

    private void assertFailedWith(String reason) {
        List<OutboxEntry> events = outbox.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getType()).isEqualTo("TransferFailed");
        assertThat(reasonOf(events.getFirst())).isEqualTo(reason);
    }

    private String reasonOf(OutboxEntry entry) {
        try {
            JsonNode node = objectMapper.readTree(entry.getPayload());
            return node.path("reason").asText(null);
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse outbox payload", e);
        }
    }
}
