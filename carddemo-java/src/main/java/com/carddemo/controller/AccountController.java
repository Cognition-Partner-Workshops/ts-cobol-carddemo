package com.carddemo.controller;

import com.carddemo.entity.Account;
import com.carddemo.entity.Customer;
import com.carddemo.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Account controller - migrated from:
 *   COACTVWC (CAVW - Account View, CICS transaction)
 *   COACTUPC (CAUP - Account Update, CICS transaction)
 *
 * Replaces CICS SEND MAP/RECEIVE MAP with REST endpoints.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * GET /api/accounts - List all accounts.
     */
    @GetMapping
    public ResponseEntity<List<Account>> listAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    /**
     * GET /api/accounts/{acctId} - View account details (CAVW transaction).
     * Includes associated customer information via card-account xref.
     */
    @GetMapping("/{acctId}")
    public ResponseEntity<Map<String, Object>> viewAccount(@PathVariable Long acctId) {
        Account account = accountService.viewAccount(acctId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Did not find this account in account master file"));

        Map<String, Object> result = new HashMap<>();
        result.put("account", account);

        accountService.getCustomerForAccount(acctId)
                .ifPresent(customer -> result.put("customer", customer));

        return ResponseEntity.ok(result);
    }

    /**
     * PUT /api/accounts/{acctId} - Update account (CAUP transaction).
     */
    @PutMapping("/{acctId}")
    public ResponseEntity<Account> updateAccount(@PathVariable Long acctId, @RequestBody Account updatedData) {
        Account updated = accountService.updateAccount(acctId, updatedData);
        return ResponseEntity.ok(updated);
    }
}
