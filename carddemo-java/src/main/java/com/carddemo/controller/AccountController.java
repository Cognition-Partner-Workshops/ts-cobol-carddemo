package com.carddemo.controller;

import com.carddemo.dto.AccountViewDto;
import com.carddemo.entity.Account;
import com.carddemo.service.AccountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountViewDto> viewAccount(@PathVariable("id") Long acctId) {
        AccountViewDto dto = accountService.getAccountView(acctId);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Account> updateAccount(@PathVariable("id") Long acctId,
                                                 @RequestBody Account account) {
        Account updated = accountService.updateAccount(acctId, account);
        return ResponseEntity.ok(updated);
    }

    @GetMapping
    public ResponseEntity<Page<Account>> listAccounts(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(accountService.listAccounts(pageable));
    }
}
