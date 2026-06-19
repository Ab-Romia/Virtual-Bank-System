package com.virtualbank.account.web;

import com.virtualbank.account.AccountService;
import com.virtualbank.account.web.dto.AccountResponse;
import com.virtualbank.account.web.dto.CreateAccountRequest;
import com.virtualbank.account.web.dto.DepositRequest;
import com.virtualbank.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        return AccountResponse.from(accountService.create(CurrentUser.requireId(), request));
    }

    @GetMapping
    public List<AccountResponse> list() {
        return accountService.listOwnedBy(CurrentUser.requireId()).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public AccountResponse byId(@PathVariable String id) {
        return AccountResponse.from(accountService.getOwned(id, CurrentUser.requireId()));
    }

    @PostMapping("/{id}/freeze")
    public AccountResponse freeze(@PathVariable String id) {
        return AccountResponse.from(accountService.freeze(id, CurrentUser.requireId()));
    }

    @PostMapping("/{id}/deposit")
    public AccountResponse deposit(@PathVariable String id, @Valid @RequestBody DepositRequest request) {
        return AccountResponse.from(accountService.deposit(id, CurrentUser.requireId(), request.amount()));
    }
}
