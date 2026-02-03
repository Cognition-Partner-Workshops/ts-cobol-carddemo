package com.carddemo.controller;

import com.carddemo.dto.ApiResponse;
import com.carddemo.model.Account;
import com.carddemo.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
    
    private final AccountService accountService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<Account>>> getAllAccounts() {
        List<Account> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }
    
    @GetMapping("/{accountId}")
    public ResponseEntity<ApiResponse<Account>> getAccountById(@PathVariable String accountId) {
        Account account = accountService.getAccountById(accountId);
        return ResponseEntity.ok(ApiResponse.success(account));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<Account>> createAccount(@RequestBody Account account) {
        Account createdAccount = accountService.createAccount(account);
        return ResponseEntity.ok(ApiResponse.success("Account created successfully", createdAccount));
    }
    
    @PutMapping("/{accountId}")
    public ResponseEntity<ApiResponse<Account>> updateAccount(
            @PathVariable String accountId,
            @RequestBody Account accountDetails) {
        Account updatedAccount = accountService.updateAccount(accountId, accountDetails);
        return ResponseEntity.ok(ApiResponse.success("Account updated successfully", updatedAccount));
    }
    
    @DeleteMapping("/{accountId}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@PathVariable String accountId) {
        accountService.deleteAccount(accountId);
        return ResponseEntity.ok(ApiResponse.success("Account deleted successfully", null));
    }
}
