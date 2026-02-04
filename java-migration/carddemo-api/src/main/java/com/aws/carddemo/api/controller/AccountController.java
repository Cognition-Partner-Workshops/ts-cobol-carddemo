package com.aws.carddemo.api.controller;

import com.aws.carddemo.service.account.AccountService;
import com.aws.carddemo.service.dto.AccountDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account management endpoints - migrated from COACTVWC (CAVW) and COACTUPC (CAUP)")
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @Operation(summary = "List all accounts with pagination")
    public ResponseEntity<Page<AccountDTO>> listAccounts(Pageable pageable) {
        return ResponseEntity.ok(accountService.listAccounts(pageable));
    }

    @GetMapping("/active")
    @Operation(summary = "List active accounts")
    public ResponseEntity<Page<AccountDTO>> listActiveAccounts(Pageable pageable) {
        return ResponseEntity.ok(accountService.listActiveAccounts(pageable));
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get account by ID")
    public ResponseEntity<AccountDTO> getAccount(@PathVariable Long accountId) {
        return accountService.getAccount(accountId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{accountId}/with-customer")
    @Operation(summary = "Get account with customer details")
    public ResponseEntity<AccountService.AccountWithCustomerDTO> getAccountWithCustomer(@PathVariable Long accountId) {
        return accountService.getAccountWithCustomer(accountId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{accountId}")
    @Operation(summary = "Update account")
    public ResponseEntity<AccountDTO> updateAccount(
            @PathVariable Long accountId,
            @Valid @RequestBody AccountService.AccountUpdateRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(accountId, request));
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "Find accounts by group")
    public ResponseEntity<Page<AccountDTO>> findByGroup(@PathVariable String groupId, Pageable pageable) {
        return ResponseEntity.ok(accountService.findByGroup(groupId, pageable));
    }

    @GetMapping("/over-limit")
    @Operation(summary = "Find over-limit accounts")
    public ResponseEntity<List<AccountDTO>> findOverLimitAccounts() {
        return ResponseEntity.ok(accountService.findOverLimitAccounts());
    }

    @GetMapping("/expiring")
    @Operation(summary = "Find accounts expiring within specified days")
    public ResponseEntity<List<AccountDTO>> findExpiringAccounts(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(accountService.findExpiringAccounts(java.time.LocalDate.now().plusDays(days)));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get account statistics")
    public ResponseEntity<AccountService.AccountStatistics> getStatistics() {
        return ResponseEntity.ok(accountService.getStatistics());
    }
}
