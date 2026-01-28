package com.carddemo.account.controller;

import com.carddemo.account.dto.AccountSummaryDto;
import com.carddemo.account.dto.CreateAccountRequest;
import com.carddemo.account.dto.UpdateAccountRequest;
import com.carddemo.account.service.AccountService;
import com.carddemo.common.dto.AccountDto;
import com.carddemo.common.dto.ApiResponse;
import com.carddemo.common.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Account management endpoints")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    @Operation(summary = "Get all accounts", description = "Get paginated list of all accounts")
    public ResponseEntity<ApiResponse<PagedResponse<AccountDto>>> getAllAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "accountId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        PagedResponse<AccountDto> accounts = accountService.getAllAccounts(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get account by ID", description = "Get account details by account ID")
    public ResponseEntity<ApiResponse<AccountDto>> getAccountById(@PathVariable Long accountId) {
        AccountDto account = accountService.getAccountById(accountId);
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get accounts by customer", description = "Get all accounts for a customer")
    public ResponseEntity<ApiResponse<List<AccountDto>>> getAccountsByCustomerId(@PathVariable Long customerId) {
        List<AccountDto> accounts = accountService.getAccountsByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @PostMapping
    @Operation(summary = "Create account", description = "Create a new account")
    public ResponseEntity<ApiResponse<AccountDto>> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountDto account = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully", account));
    }

    @PutMapping("/{accountId}")
    @Operation(summary = "Update account", description = "Update an existing account")
    public ResponseEntity<ApiResponse<AccountDto>> updateAccount(
            @PathVariable Long accountId,
            @Valid @RequestBody UpdateAccountRequest request) {
        AccountDto account = accountService.updateAccount(accountId, request);
        return ResponseEntity.ok(ApiResponse.success("Account updated successfully", account));
    }

    @PostMapping("/{accountId}/activate")
    @Operation(summary = "Activate account", description = "Activate an account")
    public ResponseEntity<ApiResponse<AccountDto>> activateAccount(@PathVariable Long accountId) {
        AccountDto account = accountService.activateAccount(accountId);
        return ResponseEntity.ok(ApiResponse.success("Account activated successfully", account));
    }

    @PostMapping("/{accountId}/deactivate")
    @Operation(summary = "Deactivate account", description = "Deactivate an account")
    public ResponseEntity<ApiResponse<AccountDto>> deactivateAccount(@PathVariable Long accountId) {
        AccountDto account = accountService.deactivateAccount(accountId);
        return ResponseEntity.ok(ApiResponse.success("Account deactivated successfully", account));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active accounts", description = "Get all active accounts")
    public ResponseEntity<ApiResponse<List<AccountDto>>> getActiveAccounts() {
        List<AccountDto> accounts = accountService.getActiveAccounts();
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/over-limit")
    @Operation(summary = "Get over-limit accounts", description = "Get all accounts over their credit limit")
    public ResponseEntity<ApiResponse<List<AccountDto>>> getOverLimitAccounts() {
        List<AccountDto> accounts = accountService.getOverLimitAccounts();
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/expiring")
    @Operation(summary = "Get expiring accounts", description = "Get accounts expiring within specified days")
    public ResponseEntity<ApiResponse<List<AccountDto>>> getExpiringAccounts(
            @RequestParam(defaultValue = "30") int daysAhead) {
        List<AccountDto> accounts = accountService.getExpiringAccounts(daysAhead);
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get account summary", description = "Get summary statistics for all accounts")
    public ResponseEntity<ApiResponse<AccountSummaryDto>> getAccountSummary() {
        AccountSummaryDto summary = accountService.getAccountSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
