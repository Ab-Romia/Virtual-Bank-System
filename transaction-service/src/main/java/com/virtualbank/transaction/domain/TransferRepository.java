package com.virtualbank.transaction.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, String> {

    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);

    List<Transfer> findByInitiatorIdOrderByCreatedAtDesc(String initiatorId);
}
