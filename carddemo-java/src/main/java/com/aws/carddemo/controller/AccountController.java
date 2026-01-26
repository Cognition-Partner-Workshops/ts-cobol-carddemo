package com.aws.carddemo.controller;

import com.aws.carddemo.dto.AccountDto;
import com.aws.carddemo.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{acctId}")
    public ResponseEntity<AccountDto> getAccount(@PathVariable Long acctId) {
        return ResponseEntity.ok(accountService.getAccount(acctId));
    }

    @GetMapping("/{acctId}/with-cards")
    public ResponseEntity<AccountDto> getAccountWithCards(@PathVariable Long acctId) {
        return ResponseEntity.ok(accountService.getAccountWithCards(acctId));
    }

    @GetMapping
    public ResponseEntity<Page<AccountDto>> getAllAccounts(Pageable pageable) {
        return ResponseEntity.ok(accountService.getAllAccounts(pageable));
    }

    @GetMapping("/active")
    public ResponseEntity<Page<AccountDto>> getActiveAccounts(Pageable pageable) {
        return ResponseEntity.ok(accountService.getActiveAccounts(pageable));
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<AccountDto>> getAccountsByGroupId(@PathVariable String groupId) {
        return ResponseEntity.ok(accountService.getAccountsByGroupId(groupId));
    }

    @GetMapping("/expired")
    public ResponseEntity<List<AccountDto>> getExpiredAccounts() {
        return ResponseEntity.ok(accountService.getExpiredAccounts());
    }

    @GetMapping("/expiring")
    public ResponseEntity<List<AccountDto>> getAccountsExpiringBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(accountService.getAccountsExpiringBetween(startDate, endDate));
    }

    @GetMapping("/overlimit")
    public ResponseEntity<List<AccountDto>> getOverlimitAccounts() {
        return ResponseEntity.ok(accountService.getOverlimitAccounts());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountDto> createAccount(@Valid @RequestBody AccountDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(dto));
    }

    @PutMapping("/{acctId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountDto> updateAccount(@PathVariable Long acctId, @Valid @RequestBody AccountDto dto) {
        return ResponseEntity.ok(accountService.updateAccount(acctId, dto));
    }

    @PatchMapping("/{acctId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateAccount(@PathVariable Long acctId) {
        accountService.deactivateAccount(acctId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count/active")
    public ResponseEntity<Long> countActiveAccounts() {
        return ResponseEntity.ok(accountService.countActiveAccounts());
    }
}
