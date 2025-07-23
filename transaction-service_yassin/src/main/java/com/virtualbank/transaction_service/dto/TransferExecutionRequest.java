package com.virtualbank.transaction_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class TransferExecutionRequest {
    @NotNull(message = "Transaction ID is required")
    private UUID transactionId;
}
