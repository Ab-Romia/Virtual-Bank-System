package com.virtualbank.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEntry, String> {

    /** The relay drains unsent rows oldest first so per-aggregate ordering is preserved. */
    List<OutboxEntry> findTop100BySentAtIsNullOrderByCreatedAtAsc();
}
