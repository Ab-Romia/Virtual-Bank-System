package com.virtualbank.audit.domain;

/** The point in a transfer's life that an audit entry records. */
public enum AuditEventType {
    REQUESTED,
    COMPLETED,
    FAILED
}
