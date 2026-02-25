package com.cardemo.controller;

import com.cardemo.dto.TransactionRequest;
import com.cardemo.entity.Transaction;
import com.cardemo.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transaction management controller.
 * Migrated from CT00 (COTRN00C - list), CT01 (COTRN01C - detail),
 * CT02 (COTRN02C - add), CR00 (CORPT00C - report).
 */
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * GET /transactions?cardNum=... - Migrated from CT00 (COTRN00C) transaction list.
     */
    @GetMapping
    public ResponseEntity<Page<Transaction>> getTransactions(
            @RequestParam("cardNum") String cardNum, Pageable pageable) {
        return ResponseEntity.ok(transactionService.getTransactionsByCardNum(cardNum, pageable));
    }

    /**
     * GET /transactions/{id} - Migrated from CT01 (COTRN01C) transaction detail.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransaction(@PathVariable("id") String id) {
        return ResponseEntity.ok(transactionService.getTransaction(id));
    }

    /**
     * POST /transactions - Migrated from CT02 (COTRN02C) transaction add.
     */
    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@Valid @RequestBody TransactionRequest request) {
        Transaction created = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
