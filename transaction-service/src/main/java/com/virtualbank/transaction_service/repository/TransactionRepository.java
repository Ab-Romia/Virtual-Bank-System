package com.virtualbank.transaction_service.repository;

import com.virtualbank.transaction_service.entity.Transaction;
import com.virtualbank.transaction_service.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByTransactionId(UUID transactionId);
    List<Transaction> findByFromAccountIdOrToAccountId(UUID accountId1, UUID accountId2);
    List<Transaction> findByFromAccountIdOrToAccountIdAndStatus(
            UUID fromAccountId, UUID toAccountId, TransactionStatus status);
}