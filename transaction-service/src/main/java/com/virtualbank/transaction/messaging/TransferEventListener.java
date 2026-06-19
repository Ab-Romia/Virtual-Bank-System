package com.virtualbank.transaction.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualbank.common.event.TransferEvent;
import com.virtualbank.common.messaging.Topics;
import com.virtualbank.transaction.TransferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumes outcomes account-service publishes on transfer.events and records them
 * in the ledger. The listener delegates to the transactional service so each
 * outcome is applied once and never moves a finished transfer. A message that
 * cannot be processed is retried by the container's error handler and finally
 * sent to transfer.events.DLT.
 */
@Component
public class TransferEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransferEventListener.class);

    private final TransferService transferService;
    private final ObjectMapper objectMapper;

    public TransferEventListener(TransferService transferService, ObjectMapper objectMapper) {
        this.transferService = transferService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = Topics.TRANSFER_EVENTS, groupId = "transaction-service")
    public void onTransferEvent(String payload) {
        TransferEvent event = parse(payload);
        switch (event) {
            case TransferEvent.TransferCompleted completed ->
                    transferService.completeTransfer(completed.transferId());
            case TransferEvent.TransferFailed failed ->
                    transferService.failTransfer(failed.transferId(), failed.reason());
        }
    }

    private TransferEvent parse(String payload) {
        try {
            return objectMapper.readValue(payload, TransferEvent.class);
        } catch (IOException e) {
            log.error("Could not parse transfer event: {}", payload);
            throw new IllegalArgumentException("Malformed transfer event", e);
        }
    }
}
