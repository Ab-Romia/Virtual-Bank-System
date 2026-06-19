package com.virtualbank.assistant.web;

/** The assistant's natural-language answer. Never returns raw fetched account data. */
public record ChatResponse(String reply) {
}
