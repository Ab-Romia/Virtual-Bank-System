package com.virtualbank.common.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Polls the outbox and forwards unsent rows to Kafka, marking each row sent only
 * after the broker acknowledges it. Runs on a short fixed delay. In production
 * this would typically be replaced by change-data-capture (Debezium); the
 * polling relay is used here because it needs no extra infrastructure and makes
 * the pattern obvious. Enabled by {@code vbank.outbox.enabled=true}.
 */
@Component
@ConditionalOnProperty(prefix = "vbank.outbox", name = "enabled", havingValue = "true")
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository repository;
    private final KafkaTemplate<String, String> kafka;
    private final Clock clock;

    public OutboxRelay(OutboxRepository repository, KafkaTemplate<String, String> kafka) {
        this(repository, kafka, Clock.systemUTC());
    }

    OutboxRelay(OutboxRepository repository, KafkaTemplate<String, String> kafka, Clock clock) {
        this.repository = repository;
        this.kafka = kafka;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${vbank.outbox.poll-interval-ms:500}")
    @Transactional
    public void publishPending() {
        List<OutboxEntry> pending = repository.findTop100BySentAtIsNullOrderByCreatedAtAsc();
        for (OutboxEntry entry : pending) {
            try {
                kafka.send(entry.getTopic(), entry.getMessageKey(), entry.getPayload())
                        .get(10, TimeUnit.SECONDS);
                entry.markSent(Instant.now(clock));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                entry.recordFailedAttempt();
                log.warn("Outbox relay interrupted while publishing {}", entry.getId());
                return;
            } catch (Exception e) {
                entry.recordFailedAttempt();
                log.warn("Outbox relay could not publish {} (attempt {}), will retry",
                        entry.getId(), entry.getAttempts(), e);
            }
        }
    }
}
