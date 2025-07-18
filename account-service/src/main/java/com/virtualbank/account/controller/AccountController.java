package com.virtualbank.account.controller;

import com.virtualbank.account.dto.AccountResponse;
import com.virtualbank.account.dto.TransferRequest;
import com.virtualbank.account.dto.TransferResponse;
import com.virtualbank.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PutMapping("/transfer")
    public ResponseEntity<TransferResponse> transferFunds(@Valid @RequestBody TransferRequest request) {
        TransferResponse response = accountService.transferFunds(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountId) {
        try {
            UUID id = UUID.fromString(accountId);
            AccountResponse account = accountService.getAccount(id);
            return ResponseEntity.ok(account);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid UUID format: " + accountId);
            throw new com.virtualbank.account.exception.AccountNotFoundException(
                    "Invalid account ID format: " + accountId);
        }
    }
}