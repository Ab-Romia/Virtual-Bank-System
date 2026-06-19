package com.virtualbank.assistant.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A single question from the authenticated caller. */
public record ChatRequest(
        @NotBlank @Size(max = 2000) String message) {
}
