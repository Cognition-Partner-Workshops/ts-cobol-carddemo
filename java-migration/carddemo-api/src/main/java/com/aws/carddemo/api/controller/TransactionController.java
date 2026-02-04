package com.aws.carddemo.api.controller;

import com.aws.carddemo.service.dto.TransactionDTO;
import com.aws.carddemo.service.transaction.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction management endpoints - migrated from COTRN00C (CT00), COTRN01C, COTRN02C (CT02)")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    @Operation(summary = "List all transactions with pagination")
    public ResponseEntity<Page<TransactionDTO>> listTransactions(Pageable pageable) {
        return ResponseEntity.ok(transactionService.listTransactions(pageable));
    }

    @GetMapping("/card/{cardNumber}")
    @Operation(summary = "List transactions by card number")
    public ResponseEntity<Page<TransactionDTO>> listTransactionsByCard(
            @PathVariable String cardNumber,
            Pageable pageable) {
        return ResponseEntity.ok(transactionService.listTransactionsByCard(cardNumber, pageable));
    }

    @GetMapping("/date-range")
    @Operation(summary = "List transactions by date range")
    public ResponseEntity<Page<TransactionDTO>> listTransactionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable) {
        return ResponseEntity.ok(transactionService.listTransactionsByDateRange(startDate, endDate, pageable));
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<TransactionDTO> getTransaction(@PathVariable String transactionId) {
        return transactionService.getTransaction(transactionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Add new transaction")
    public ResponseEntity<TransactionDTO> addTransaction(
            @Valid @RequestBody TransactionService.TransactionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.addTransaction(request));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get transaction summary by type")
    public ResponseEntity<List<TransactionService.TransactionSummary>> getTransactionSummaryByType(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(transactionService.getTransactionSummaryByType(startDate, endDate));
    }

    @GetMapping("/large")
    @Operation(summary = "Find large transactions above threshold")
    public ResponseEntity<List<TransactionDTO>> findLargeTransactions(
            @RequestParam BigDecimal threshold) {
        return ResponseEntity.ok(transactionService.findLargeTransactions(threshold));
    }

    @GetMapping("/unprocessed")
    @Operation(summary = "Find unprocessed transactions")
    public ResponseEntity<List<TransactionDTO>> findUnprocessedTransactions() {
        return ResponseEntity.ok(transactionService.findUnprocessedTransactions());
    }

    @PostMapping("/{transactionId}/process")
    @Operation(summary = "Mark transaction as processed")
    public ResponseEntity<Void> markAsProcessed(@PathVariable String transactionId) {
        transactionService.markAsProcessed(transactionId);
        return ResponseEntity.ok().build();
    }
}
