package com.virtualbank.account.web.dto;

import com.virtualbank.account.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Body of POST /accounts. The owner is taken from the token, not the request. */
public record CreateAccountRequest(
        @NotNull AccountType type,
        @NotBlank String currency
) {
}
