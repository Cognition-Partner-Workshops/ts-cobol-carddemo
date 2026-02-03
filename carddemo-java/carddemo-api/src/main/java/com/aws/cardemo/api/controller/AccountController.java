package com.aws.cardemo.api.controller;

import com.aws.cardemo.domain.entity.Account;
import com.aws.cardemo.services.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Account", description = "Account management APIs")
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @Operation(summary = "Get all accounts")
    public ResponseEntity<List<Account>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get account by ID")
    public ResponseEntity<Account> getAccountById(@PathVariable String accountId) {
        return accountService.getAccountById(accountId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new account")
    public ResponseEntity<Account> createAccount(@Valid @RequestBody Account account) {
        Account created = accountService.createAccount(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{accountId}")
    @Operation(summary = "Update an existing account")
    public ResponseEntity<Account> updateAccount(
            @PathVariable String accountId,
            @Valid @RequestBody Account account) {
        account.setAccountId(accountId);
        Account updated = accountService.updateAccount(account);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{accountId}")
    @Operation(summary = "Delete an account")
    public ResponseEntity<Void> deleteAccount(@PathVariable String accountId) {
        accountService.deleteAccount(accountId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get accounts by status")
    public ResponseEntity<List<Account>> getAccountsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(accountService.getAccountsByStatus(status));
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "Get accounts by group ID")
    public ResponseEntity<List<Account>> getAccountsByGroupId(@PathVariable String groupId) {
        return ResponseEntity.ok(accountService.getAccountsByGroupId(groupId));
    }

    @GetMapping("/over-limit")
    @Operation(summary = "Get accounts over credit limit")
    public ResponseEntity<List<Account>> getAccountsOverCreditLimit() {
        return ResponseEntity.ok(accountService.getAccountsOverCreditLimit());
    }
}
