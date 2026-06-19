package com.virtualbank.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;

/**
 * Appends an event to the outbox. Call this inside the same transaction as the
 * state change it describes; the {@link OutboxRelay} publishes it afterwards.
 * Wired by VbankMessagingAutoConfiguration when vbank.outbox.enabled is true.
 */
public class OutboxAppender {

    private final OutboxRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxAppender(OutboxRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * @param eventId stable id used as the outbox row id and the consumer's idempotency key
     * @param topic   destination Kafka topic
     * @param key     partition key (the transferId) so a transfer's messages stay ordered
     * @param event   the event payload, serialized to JSON
     */
    public void append(String eventId, String topic, String key, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEntry entry = new OutboxEntry(eventId, topic, key, payload,
                    event.getClass().getSimpleName(), Instant.now(clock));
            repository.save(entry);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize outbox event " + event.getClass(), e);
        }
    }
}
