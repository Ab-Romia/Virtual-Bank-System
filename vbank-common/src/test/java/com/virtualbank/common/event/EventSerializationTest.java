package com.virtualbank.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The saga depends on the request and result messages surviving a JSON round
 * trip, including the polymorphic result type. If that breaks, transfers break,
 * so this is a first-class contract test.
 */
class EventSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void transferRequestedRoundTrips() throws Exception {
        TransferRequested request = new TransferRequested(
                "tx-1", "user-1", "acc-from", "acc-to",
                new BigDecimal("25.00"), "USD", Instant.parse("2026-06-19T10:00:00Z"));

        TransferRequested back = mapper.readValue(mapper.writeValueAsString(request), TransferRequested.class);

        assertThat(back.transferId()).isEqualTo("tx-1");
        assertThat(back.initiatorId()).isEqualTo("user-1");
        assertThat(back.amount()).isEqualByComparingTo("25.00");
    }

    @Test
    void transferResultsRoundTripThroughTheSealedInterface() throws Exception {
        TransferEvent completed = new TransferEvent.TransferCompleted("tx-1", Instant.parse("2026-06-19T10:00:01Z"));
        String completedJson = mapper.writeValueAsString(completed);
        assertThat(completedJson).contains("\"type\":\"TransferCompleted\"");
        assertThat(mapper.readValue(completedJson, TransferEvent.class))
                .isInstanceOf(TransferEvent.TransferCompleted.class);

        TransferEvent failed = new TransferEvent.TransferFailed("tx-1", "INSUFFICIENT_FUNDS",
                Instant.parse("2026-06-19T10:00:02Z"));
        TransferEvent back = mapper.readValue(mapper.writeValueAsString(failed), TransferEvent.class);
        assertThat(back).isInstanceOf(TransferEvent.TransferFailed.class);
        assertThat(((TransferEvent.TransferFailed) back).reason()).isEqualTo("INSUFFICIENT_FUNDS");
    }
}
