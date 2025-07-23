package com.virtualbank.bff_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionsResponseDto {
    @JsonProperty("transactionId")
    private UUID transactionId;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("description")
    private String description;

    @JsonProperty("timestamp")
    private ZonedDateTime timestamp;

    @JsonProperty("status")
    private String status;

    private UUID fromAccountId;
    private UUID toAccountId;
}