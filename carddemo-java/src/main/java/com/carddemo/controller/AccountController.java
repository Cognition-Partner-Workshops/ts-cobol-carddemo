package com.carddemo.controller;

import com.carddemo.entity.Account;
import com.carddemo.service.AccountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<Page<Account>> listAccounts(Pageable pageable) {
        return ResponseEntity.ok(accountService.listAccounts(pageable));
    }

    @GetMapping("/{acctId}")
    public ResponseEntity<Map<String, Object>> getAccountView(@PathVariable Long acctId) {
        return ResponseEntity.ok(accountService.getAccountView(acctId));
    }

    @PutMapping("/{acctId}")
    public ResponseEntity<Account> updateAccount(@PathVariable Long acctId,
                                                 @RequestBody Account account) {
        return ResponseEntity.ok(accountService.updateAccount(acctId, account));
    }
}
