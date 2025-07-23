package com.virtualbank.account.repository;

import com.virtualbank.account.model.Account;
import com.virtualbank.account.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findByUserId(UUID userId);
    Account findByAccountNumber(String accountNumber);
    boolean existsByAccountNumber(String accountNumber);
    List<Account> findByStatusAndLastTransactionAtBefore(Status status, ZonedDateTime cutoffTime);
}