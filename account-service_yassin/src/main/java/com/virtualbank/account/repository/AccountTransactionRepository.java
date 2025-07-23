package com.virtualbank.account.repository;

import com.virtualbank.account.model.AccountTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, UUID> {
    List<AccountTransaction> findByFromAccountId(UUID fromAccountId);
    List<AccountTransaction> findByToAccountId(UUID toAccountId);
}