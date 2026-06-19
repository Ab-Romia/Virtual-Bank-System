package com.virtualbank.audit.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEntryRepository extends JpaRepository<AuditEntry, String> {

    boolean existsByTransferIdAndEventType(String transferId, AuditEventType eventType);

    List<AuditEntry> findByTransferIdOrderByOccurredAt(String transferId);
}
