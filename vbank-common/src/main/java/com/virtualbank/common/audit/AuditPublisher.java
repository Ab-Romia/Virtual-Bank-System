package com.virtualbank.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualbank.common.messaging.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

/**
 * Publishes {@link AuditEnvelope}s to the audit topic as JSON strings. Replaces
 * the four byte-identical KafkaProducerService classes the audit found. Audit
 * publication is best-effort and never on the request's critical path: a broker
 * hiccup logs a warning rather than failing the business operation.
 */
public class AuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(AuditPublisher.class);

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;
    private final String serviceName;
    private final Clock clock;

    public AuditPublisher(KafkaTemplate<String, String> kafka, ObjectMapper objectMapper,
                          String serviceName, Clock clock) {
        this.kafka = kafka;
        this.objectMapper = objectMapper;
        this.serviceName = serviceName;
        this.clock = clock;
    }

    public void publish(String correlationId, String action, String level, String message,
                        Map<String, Object> details) {
        AuditEnvelope envelope = new AuditEnvelope(correlationId, serviceName, action, level, message,
                details, Instant.now(clock));
        try {
            String payload = objectMapper.writeValueAsString(envelope);
            kafka.send(Topics.AUDIT_LOG, correlationId, payload);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize audit envelope for action {}", action, e);
        } catch (RuntimeException e) {
            log.warn("Could not publish audit envelope for action {}", action, e);
        }
    }
}
