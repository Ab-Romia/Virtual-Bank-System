package com.virtualbank.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The saga depends on commands and events surviving a JSON round-trip through
 * their sealed interface. If the polymorphic type discriminator breaks, the
 * whole transfer flow breaks, so this is a first-class contract test.
 */
class EventSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void debitRequestedRoundTripsThroughTheCommandInterface() throws Exception {
        TransferCommand command = new TransferCommand.DebitRequested(
                "evt-1", "tx-1", "acc-from", "acc-to",
                new BigDecimal("25.00"), "USD", Instant.parse("2026-06-19T10:00:00Z"));

        String json = mapper.writeValueAsString(command);
        assertThat(json).contains("\"type\":\"DebitRequested\"");

        TransferCommand back = mapper.readValue(json, TransferCommand.class);
        assertThat(back).isInstanceOf(TransferCommand.DebitRequested.class);
        assertThat(back.transferId()).isEqualTo("tx-1");
        assertThat(((TransferCommand.DebitRequested) back).amount()).isEqualByComparingTo("25.00");
    }

    @Test
    void creditFailedRoundTripsThroughTheEventInterface() throws Exception {
        TransferEvent event = new TransferEvent.CreditFailed(
                "evt-2", "tx-1", "acc-to", "DESTINATION_INACTIVE",
                Instant.parse("2026-06-19T10:00:01Z"));

        String json = mapper.writeValueAsString(event);
        assertThat(json).contains("\"type\":\"CreditFailed\"");

        TransferEvent back = mapper.readValue(json, TransferEvent.class);
        assertThat(back).isInstanceOf(TransferEvent.CreditFailed.class);
        assertThat(((TransferEvent.CreditFailed) back).reason()).isEqualTo("DESTINATION_INACTIVE");
    }
}
