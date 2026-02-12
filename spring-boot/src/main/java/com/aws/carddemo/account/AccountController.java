package com.aws.carddemo.account;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aws.carddemo.account.dto.AccountResponse;
import com.aws.carddemo.account.dto.AccountUpdateRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.getAccount(accountId));
    }

    @PutMapping("/{accountId}")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable Long accountId,
            @Valid @RequestBody AccountUpdateRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(accountId, request));
    }

    @GetMapping
    public ResponseEntity<Page<AccountResponse>> listAccounts(
            @RequestParam(required = false) Long customerId,
            Pageable pageable) {
        if (customerId != null) {
            return ResponseEntity.ok(accountService.listAccountsByCustomer(customerId, pageable));
        }
        return ResponseEntity.ok(accountService.listAll(pageable));
    }
}
