package com.virtualbank.transaction_service.dto;

import com.virtualbank.transaction_service.entity.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferExecutionResponse {
    private UUID transactionId;
    private TransactionStatus status;
    private ZonedDateTime timestamp;
}
