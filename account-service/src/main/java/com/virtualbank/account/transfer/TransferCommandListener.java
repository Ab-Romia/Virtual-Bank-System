package com.virtualbank.account.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualbank.common.event.TransferRequested;
import com.virtualbank.common.messaging.Topics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes transfer commands. The message value is the JSON of a
 * {@link TransferRequested}; this parses it and hands it to {@link TransferService}.
 * A message that cannot be parsed or that the handler keeps failing on is retried
 * a few times and then routed to transfer.commands.DLT by the container's error
 * handler, so one poison record never blocks the partition.
 */
@Component
public class TransferCommandListener {

    private final TransferService transferService;
    private final ObjectMapper objectMapper;

    public TransferCommandListener(TransferService transferService, ObjectMapper objectMapper) {
        this.transferService = transferService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = Topics.TRANSFER_COMMANDS, groupId = "account-service")
    public void onCommand(String payload) throws Exception {
        TransferRequested command = objectMapper.readValue(payload, TransferRequested.class);
        transferService.apply(command);
    }
}
