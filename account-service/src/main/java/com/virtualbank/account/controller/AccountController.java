package com.virtualbank.account.controller;

import com.virtualbank.account.dto.*;
import com.virtualbank.account.exception.InvalidCreationException;
import com.virtualbank.account.exception.UserNotFoundException;
import com.virtualbank.account.scheduler.AccountInactiveJob;
import com.virtualbank.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PutMapping("/accounts/transfer")
    public ResponseEntity<TransferResponse> transferFunds(@Valid @RequestBody TransferRequest request) {
        TransferResponse response = accountService.transferFunds(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts/{accountId}")
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


    @PostMapping("/accounts")
    public ResponseEntity<?> createAccount(@Valid @RequestBody CreateRequest request){
        try{
            CreateResponse response = accountService.createAccount(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (UserNotFoundException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", 404);
            errorResponse.put("error", "Not Found");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e){
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", 400);
            errorResponse.put("error", "Bad Request");
            errorResponse.put("message", "Invalid account type or initial balance");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @RestController
    @RequestMapping("/accounts/admin")
    public class AccountAdminController {

        private final AccountInactiveJob accountInactiveJob;

        public AccountAdminController(AccountInactiveJob accountInactiveJob) {
            this.accountInactiveJob = accountInactiveJob;
        }

        @PostMapping("/run-inactivation")
        public ResponseEntity<Map<String, Object>> triggerInactivation() {
            accountInactiveJob.inactivateStaleAccounts();
            return ResponseEntity.ok(Map.of(
                    "timestamp", ZonedDateTime.now().toString(),
                    "message", "Stale account inactivation job executed"
            ));
        }
    }
    @GetMapping("/users/{userId}/accounts")
    public ResponseEntity<List<UserAccountsResponse>> findUserAccounts(@PathVariable String userId){
        try{
            UUID id = UUID.fromString(userId);
            List<UserAccountsResponse> response = accountService.findUserAccounts(id);
            return ResponseEntity.ok(response);
        }catch(IllegalArgumentException e){
            System.err.println("Invalid UUID format: " + userId);
            throw new com.virtualbank.account.exception.AccountNotFoundException(
                    "Invalid user ID format: " + userId);
        }

    }
}