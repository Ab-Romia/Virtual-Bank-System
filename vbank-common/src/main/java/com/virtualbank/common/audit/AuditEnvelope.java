package com.virtualbank.common.audit;

import java.time.Instant;
import java.util.Map;

/**
 * The canonical audit record every service publishes to the {@code audit.log}
 * topic. The audit-service persists these for a centralized, tamper-evident
 * trail. The correlationId ties an audit entry back to the request and trace
 * that produced it.
 */
public record AuditEnvelope(
        String correlationId,
        String service,
        String action,
        String level,
        String message,
        Map<String, Object> details,
        Instant timestamp
) {
}
