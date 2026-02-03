package com.aws.cardemo.api.controller;

import com.aws.cardemo.domain.entity.Transaction;
import com.aws.cardemo.services.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction", description = "Transaction management APIs")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    @Operation(summary = "Get all transactions")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable String transactionId) {
        return transactionService.getTransactionById(transactionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new transaction")
    public ResponseEntity<Transaction> createTransaction(@Valid @RequestBody Transaction transaction) {
        Transaction created = transactionService.createTransaction(transaction);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{transactionId}")
    @Operation(summary = "Update an existing transaction")
    public ResponseEntity<Transaction> updateTransaction(
            @PathVariable String transactionId,
            @Valid @RequestBody Transaction transaction) {
        transaction.setTransactionId(transactionId);
        Transaction updated = transactionService.updateTransaction(transaction);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{transactionId}")
    @Operation(summary = "Delete a transaction")
    public ResponseEntity<Void> deleteTransaction(@PathVariable String transactionId) {
        transactionService.deleteTransaction(transactionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/card/{cardNumber}")
    @Operation(summary = "Get transactions by card number")
    public ResponseEntity<List<Transaction>> getTransactionsByCardNumber(@PathVariable String cardNumber) {
        return ResponseEntity.ok(transactionService.getTransactionsByCardNumber(cardNumber));
    }

    @GetMapping("/card/{cardNumber}/paged")
    @Operation(summary = "Get transactions by card number with pagination")
    public ResponseEntity<Page<Transaction>> getTransactionsByCardNumberPaged(
            @PathVariable String cardNumber,
            Pageable pageable) {
        return ResponseEntity.ok(transactionService.getTransactionsByCardNumber(cardNumber, pageable));
    }

    @GetMapping("/card/{cardNumber}/date-range")
    @Operation(summary = "Get transactions by card number and date range")
    public ResponseEntity<List<Transaction>> getTransactionsByCardNumberAndDateRange(
            @PathVariable String cardNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(
                transactionService.getTransactionsByCardNumberAndDateRange(cardNumber, startDate, endDate));
    }

    @GetMapping("/merchant/{merchantId}")
    @Operation(summary = "Get transactions by merchant ID")
    public ResponseEntity<List<Transaction>> getTransactionsByMerchantId(@PathVariable String merchantId) {
        return ResponseEntity.ok(transactionService.getTransactionsByMerchantId(merchantId));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get transactions from the last 24 hours")
    public ResponseEntity<List<Transaction>> getRecentTransactions() {
        return ResponseEntity.ok(transactionService.getTransactionsLast24Hours());
    }
}
