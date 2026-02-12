package com.carddemo.controller;

import com.carddemo.service.AccountService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for account management.
 * Replaces CICS transaction CAVW (COACTVWC.cbl - Account View)
 * and related account management CICS screens.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }
}
