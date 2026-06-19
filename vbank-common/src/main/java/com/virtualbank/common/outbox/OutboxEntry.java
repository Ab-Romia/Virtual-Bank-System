package com.virtualbank.common.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A row in a service's transactional outbox. The business state change and the
 * insertion of this row happen in the same local transaction, so an event is
 * never lost and never published without its state change committing. A polling
 * {@link OutboxRelay} forwards unsent rows to Kafka. The id is the message's
 * eventId, which the consumer uses for idempotent processing.
 */
@Entity
@Table(name = "outbox")
public class OutboxEntry {

    @Id
    private String id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "message_key", nullable = false)
    private String messageKey;

    @Column(columnDefinition = "text", nullable = false)
    private String payload;

    @Column(nullable = false)
    private String type;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(nullable = false)
    private int attempts;

    protected OutboxEntry() {
    }

    public OutboxEntry(String id, String topic, String messageKey, String payload, String type, Instant createdAt) {
        this.id = id;
        this.topic = topic;
        this.messageKey = messageKey;
        this.payload = payload;
        this.type = type;
        this.createdAt = createdAt;
        this.attempts = 0;
    }

    public void markSent(Instant when) {
        this.sentAt = when;
    }

    public void recordFailedAttempt() {
        this.attempts++;
    }

    public String getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getPayload() {
        return payload;
    }

    public String getType() {
        return type;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public int getAttempts() {
        return attempts;
    }
}
