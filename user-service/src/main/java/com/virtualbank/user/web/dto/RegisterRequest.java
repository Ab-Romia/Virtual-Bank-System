package com.virtualbank.user.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String username,
        String email,
        @NotBlank String password,
        String fullName) {
}
