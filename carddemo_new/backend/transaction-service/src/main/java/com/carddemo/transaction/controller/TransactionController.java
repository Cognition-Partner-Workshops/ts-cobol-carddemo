package com.carddemo.transaction.controller;

import com.carddemo.common.dto.ApiResponse;
import com.carddemo.common.dto.PageResponse;
import com.carddemo.transaction.dto.TransactionCreateRequest;
import com.carddemo.transaction.dto.TransactionDto;
import com.carddemo.transaction.entity.TransactionCategory;
import com.carddemo.transaction.entity.TransactionType;
import com.carddemo.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Management", description = "Transaction list, view, and add operations - EPIC-004")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/{transactionId}")
    @Operation(summary = "View transaction details", description = "Display single transaction details (US-004-02-01)")
    public ResponseEntity<ApiResponse<TransactionDto>> getTransaction(@PathVariable String transactionId) {
        TransactionDto transaction = transactionService.getTransactionById(transactionId);
        return ResponseEntity.ok(ApiResponse.success(transaction));
    }

    @GetMapping
    @Operation(summary = "List all transactions", description = "Get paginated list of all transactions (US-004-01-01)")
    public ResponseEntity<ApiResponse<PageResponse<TransactionDto>>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<TransactionDto> transactions = transactionService.getAllTransactions(page, size);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "List transactions by account", description = "Get transactions for a specific account (US-004-01-02)")
    public ResponseEntity<ApiResponse<PageResponse<TransactionDto>>> getTransactionsByAccount(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<TransactionDto> transactions = transactionService.getTransactionsByAccount(accountId, page, size);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/card/{cardNumber}")
    @Operation(summary = "List transactions by card", description = "Get transactions for a specific card")
    public ResponseEntity<ApiResponse<PageResponse<TransactionDto>>> getTransactionsByCard(
            @PathVariable String cardNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<TransactionDto> transactions = transactionService.getTransactionsByCard(cardNumber, page, size);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/account/{accountId}/range")
    @Operation(summary = "List transactions by date range", description = "Get transactions for an account within a date range (US-004-01-03)")
    public ResponseEntity<ApiResponse<PageResponse<TransactionDto>>> getTransactionsByDateRange(
            @PathVariable String accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<TransactionDto> transactions = transactionService.getTransactionsByAccountAndDateRange(
                accountId, startDate, endDate, page, size);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @PostMapping
    @Operation(summary = "Add new transaction", description = "Create a new transaction (US-004-03-01 to US-004-03-06)")
    public ResponseEntity<ApiResponse<TransactionDto>> createTransaction(
            @Valid @RequestBody TransactionCreateRequest request) {
        TransactionDto transaction = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(transaction, "Transaction created successfully"));
    }

    @GetMapping("/types")
    @Operation(summary = "Get transaction types", description = "Get all available transaction types")
    public ResponseEntity<ApiResponse<List<TransactionType>>> getTransactionTypes() {
        List<TransactionType> types = transactionService.getAllTransactionTypes();
        return ResponseEntity.ok(ApiResponse.success(types));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get transaction categories", description = "Get all available transaction categories")
    public ResponseEntity<ApiResponse<List<TransactionCategory>>> getTransactionCategories() {
        List<TransactionCategory> categories = transactionService.getAllTransactionCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }
}
