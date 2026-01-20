package com.carddemo.account.controller;

import com.carddemo.account.dto.AccountDto;
import com.carddemo.account.dto.AccountUpdateRequest;
import com.carddemo.account.service.AccountService;
import com.carddemo.common.dto.ApiResponse;
import com.carddemo.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Account Management", description = "Account view and update operations - EPIC-002")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/{accountId}")
    @Operation(summary = "View account details", description = "Display account information including balances and credit limits (US-002-01-01)")
    public ResponseEntity<ApiResponse<AccountDto>> getAccount(@PathVariable String accountId) {
        AccountDto account = accountService.getAccountById(accountId);
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @GetMapping
    @Operation(summary = "List all accounts", description = "Get paginated list of all accounts")
    public ResponseEntity<ApiResponse<PageResponse<AccountDto>>> getAllAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "accountId") String sortBy) {
        PageResponse<AccountDto> accounts = accountService.getAllAccounts(page, size, sortBy);
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get accounts by customer", description = "Get all accounts for a specific customer (US-002-01-02)")
    public ResponseEntity<ApiResponse<List<AccountDto>>> getAccountsByCustomer(@PathVariable String customerId) {
        List<AccountDto> accounts = accountService.getAccountsByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @PutMapping("/{accountId}")
    @Operation(summary = "Update account", description = "Modify account details including credit limits (US-002-02-01, US-002-02-02)")
    public ResponseEntity<ApiResponse<AccountDto>> updateAccount(
            @PathVariable String accountId,
            @Valid @RequestBody AccountUpdateRequest request) {
        AccountDto account = accountService.updateAccount(accountId, request);
        return ResponseEntity.ok(ApiResponse.success(account, "Account updated successfully"));
    }
}
