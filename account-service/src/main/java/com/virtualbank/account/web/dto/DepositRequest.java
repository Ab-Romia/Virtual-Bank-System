package com.virtualbank.account.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** A deposit into one of the caller's own accounts (a sandbox top-up). */
public record DepositRequest(@NotNull @Positive BigDecimal amount) {
}
