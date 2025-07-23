package com.virtualbank.account.service;

import com.virtualbank.account.dto.*;
import com.virtualbank.account.exception.AccountNotFoundException;
import com.virtualbank.account.exception.InsufficientFundsException;
import com.virtualbank.account.exception.InvalidCreationException;
import com.virtualbank.account.model.Account;
import com.virtualbank.account.model.AccountTransaction;
import com.virtualbank.account.model.Status;
import com.virtualbank.account.repository.AccountRepository;
import com.virtualbank.account.repository.AccountTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository,
                          AccountTransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransferResponse transferFunds(TransferRequest request) {
        // Find accounts
        Account fromAccount = accountRepository.findById(request.getFromAccountId())
                .orElseThrow(() -> new AccountNotFoundException("Account with ID " +
                        request.getFromAccountId() + " not found"));
        Account toAccount = accountRepository.findById(request.getToAccountId())
                .orElseThrow(() -> new AccountNotFoundException("Account with ID " +
                        request.getToAccountId() + " not found"));

        // Validate funds
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds in account " +
                    request.getFromAccountId());
        }

        // Update balances
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

        // Update last transaction timestamp
        ZonedDateTime now = ZonedDateTime.now();
        fromAccount.setLastTransactionAt(now);
        toAccount.setLastTransactionAt(now);

        // Save updated accounts
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

// Record transaction
        AccountTransaction transaction = new AccountTransaction();
        transaction.setTransactionId(UUID.randomUUID()); // Add this line
        transaction.setFromAccountId(request.getFromAccountId());
        transaction.setToAccountId(request.getToAccountId());
        transaction.setAmount(request.getAmount());
        transaction.setTransactionType("TRANSFER");
        transaction.setStatus("COMPLETED");
        transaction.setCreatedAt(ZonedDateTime.now()); // Also add this line
        transaction.setDescription("Transfer from " + fromAccount.getAccountNumber() +
                " to " + toAccount.getAccountNumber());
        transactionRepository.save(transaction);

        return new TransferResponse("Transfer completed successfully");
    }

    public  AccountResponse getAccount(UUID accountId) {
        System.out.println("Looking for account with ID: " + accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    System.err.println("Account not found: " + accountId);
                    return new AccountNotFoundException("Account with ID " + accountId + " not found");
                });

        // Convert to DTO
        AccountResponse response = new AccountResponse();
        response.setAccountId(account.getAccountId());
        response.setUserId(account.getUserId());
        response.setAccountNumber(account.getAccountNumber());
        response.setAccountType(account.getAccountType());
        response.setBalance(account.getBalance());
        response.setStatus(account.getStatus());
        response.setCreatedAt(account.getCreatedAt());
        response.setUpdatedAt(account.getUpdatedAt());
        response.setLastTransactionAt(account.getLastTransactionAt());

        return response;
    }

    public String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            accountNumber = AccountNumberGenerator.generateUniqueAccountNumber();
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

    @Transactional
    public CreateResponse createAccount(CreateRequest request){
        try {
            if(request.getInitialBalance().compareTo(BigDecimal.ZERO) < 0){
                throw new InvalidCreationException("Invalid account type or balance");
            }

            //create new account for user
            Account account = Account.builder()
                    .userId(request.getUserId())
                    .accountNumber(generateUniqueAccountNumber())
                    .accountType(request.getAccountType())
                    .balance(request.getInitialBalance())
                    .status(Status.ACTIVE)
                    .createdAt(ZonedDateTime.now())
                    .updatedAt(ZonedDateTime.now())
                    .build();

            Account savedAcc = accountRepository.save(account);

            return CreateResponse.builder()
                    .accountId(savedAcc.getAccountId())
                    .accountNumber(savedAcc.getAccountNumber())
                    .message("Account created")
                    .build();
        } catch (Exception e) {
            throw e;
        }


    }

    @Transactional
    public List<UserAccountsResponse> findUserAccounts(UUID userId){
        List<Account> accounts = accountRepository.findByUserId(userId);

        if(accounts.isEmpty()){
            throw new AccountNotFoundException("No accounts found");
        }
       List<UserAccountsResponse> response = accounts.stream().map(UserAccountsResponse::new).collect(Collectors.toList());

        return response;
    }
}