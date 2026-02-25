package com.carddemo.controller;

import com.carddemo.dto.TransactionAddRequest;
import com.carddemo.entity.Transaction;
import com.carddemo.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
 * Transaction controller - migrated from:
 *   COTRN00C (CT00 - Transaction List)
 *   COTRN01C (CT01 - Transaction View)
 *   COTRN02C (CT02 - Transaction Add)
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * GET /api/transactions?cardNum={cardNum} - List transactions (CT00).
     * Replaces COTRN00C STARTBR/READNEXT on TRANSACT AIX.
     */
    @GetMapping
    public ResponseEntity<Page<Transaction>> listTransactions(
            @RequestParam(required = false) String cardNum,
            @PageableDefault(size = 10) Pageable pageable) {
        if (cardNum != null && !cardNum.isBlank()) {
            return ResponseEntity.ok(transactionService.listByCardNum(cardNum, pageable));
        }
        return ResponseEntity.ok(transactionService.listAll(pageable));
    }

    /**
     * GET /api/transactions/{tranId} - View transaction detail (CT01).
     * Replaces COTRN01C READ on TRANSACT.
     */
    @GetMapping("/{tranId}")
    public ResponseEntity<Transaction> viewTransaction(@PathVariable String tranId) {
        Transaction transaction = transactionService.viewTransaction(tranId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        return ResponseEntity.ok(transaction);
    }

    /**
     * POST /api/transactions - Add a new transaction (CT02).
     * Replaces COTRN02C WRITE to TRANSACT with balance updates.
     */
    @PostMapping
    public ResponseEntity<Transaction> addTransaction(@Valid @RequestBody TransactionAddRequest request) {
        Transaction created = transactionService.addTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
