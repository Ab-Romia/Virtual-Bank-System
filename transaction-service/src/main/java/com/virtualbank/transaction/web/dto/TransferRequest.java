package com.virtualbank.transaction.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * The body of POST /transfers. Structural validation lives here; the business
 * rules (amount greater than zero, distinct accounts) are checked in the service
 * so they apply no matter how a transfer is started.
 */
public record TransferRequest(
        @NotBlank String fromAccountId,
        @NotBlank String toAccountId,
        @NotNull BigDecimal amount,
        @NotBlank String currency) {
}
