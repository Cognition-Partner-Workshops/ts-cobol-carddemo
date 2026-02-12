package com.carddemo.controller;

import com.carddemo.service.TransactionService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for transaction processing.
 * Replaces CICS transaction CT02 and related transactions:
 * - COTRN00C.cbl (Transaction List)
 * - COTRN01C.cbl (Transaction View)
 * - COTRN02C.cbl (Transaction Add)
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }
}
