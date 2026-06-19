package com.virtualbank.audit.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualbank.audit.AuditService;
import com.virtualbank.common.event.TransferEvent;
import com.virtualbank.common.event.TransferRequested;
import com.virtualbank.common.messaging.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumes both transfer streams into the audit log. A TransferRequested command
 * is recorded as the REQUESTED entry with the transfer's parties and amount; a
 * TransferEvent outcome becomes the COMPLETED or FAILED entry. Recording is
 * idempotent in the service, so a redelivered message is stored once. A message
 * that cannot be parsed is retried by the container's error handler and finally
 * sent to the matching .DLT topic.
 */
@Component
public class TransferStreamListener {

    private static final Logger log = LoggerFactory.getLogger(TransferStreamListener.class);

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public TransferStreamListener(AuditService auditService, ObjectMapper objectMapper) {
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = Topics.TRANSFER_COMMANDS, groupId = "audit-service")
    public void onTransferCommand(String payload) {
        auditService.recordRequested(parse(payload, TransferRequested.class));
    }

    @KafkaListener(topics = Topics.TRANSFER_EVENTS, groupId = "audit-service")
    public void onTransferEvent(String payload) {
        auditService.recordEvent(parse(payload, TransferEvent.class));
    }

    private <T> T parse(String payload, Class<T> type) {
        try {
            return objectMapper.readValue(payload, type);
        } catch (IOException e) {
            log.error("Could not parse {} message: {}", type.getSimpleName(), payload);
            throw new IllegalArgumentException("Malformed " + type.getSimpleName() + " message", e);
        }
    }
}
