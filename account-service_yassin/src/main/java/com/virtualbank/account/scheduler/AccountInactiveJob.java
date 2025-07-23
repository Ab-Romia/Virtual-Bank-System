package com.virtualbank.account.scheduler;

import com.virtualbank.account.model.Account;
import com.virtualbank.account.model.Status;
import com.virtualbank.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountInactiveJob {
    private final AccountRepository accountRepository;

    @Scheduled(fixedRate = 5 * 60 * 1000) // for every hour fixed rate = 60 * 60 * 1000
    public void inactivateStaleAccounts(){
        ZonedDateTime cutoffTime = ZonedDateTime.now().minusMinutes(5);
        List<Account> staleAccounts = accountRepository.findByStatusAndLastTransactionAtBefore(Status.ACTIVE, cutoffTime);
        if (!staleAccounts.isEmpty()) {
            staleAccounts.forEach(account -> {
                account.setStatus(Status.INACTIVE);
                account.setUpdatedAt(ZonedDateTime.now());
            });

            accountRepository.saveAll(staleAccounts);
            log.info("Inactivated {} stale account(s).", staleAccounts.size());
        } else {
            log.info("No stale accounts found for inactivation.");
        }
    }
}
