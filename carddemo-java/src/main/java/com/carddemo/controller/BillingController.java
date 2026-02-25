package com.carddemo.controller;

import com.carddemo.dto.BillPaymentRequest;
import com.carddemo.entity.Account;
import com.carddemo.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Billing controller - migrated from COBIL00C (CB00 - Bill Payment).
 * Replaces CICS transaction CB00 with REST endpoint.
 */
@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final AccountService accountService;

    public BillingController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * POST /api/billing/payment - Process bill payment (CB00).
     * Reduces account balance and updates cycle credit.
     */
    @PostMapping("/payment")
    public ResponseEntity<Account> makePayment(@Valid @RequestBody BillPaymentRequest request) {
        Account updated = accountService.applyPayment(request.getAcctId(), request.getPaymentAmount());
        return ResponseEntity.ok(updated);
    }
}
