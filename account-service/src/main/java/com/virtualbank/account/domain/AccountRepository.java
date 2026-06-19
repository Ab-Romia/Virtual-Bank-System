package com.virtualbank.account.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {

    List<Account> findByOwnerId(String ownerId);

    boolean existsByAccountNumber(String accountNumber);

    /**
     * Loads an account taking a row-level write lock so a concurrent transfer
     * touching the same account blocks until this one commits. Used by the
     * transfer path to serialize debits and prevent double-spend.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") String id);
}
