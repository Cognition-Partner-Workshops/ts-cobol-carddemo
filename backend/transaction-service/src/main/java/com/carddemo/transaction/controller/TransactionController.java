package com.carddemo.transaction.controller;

import com.carddemo.common.dto.ApiResponse;
import com.carddemo.common.dto.PagedResponse;
import com.carddemo.common.dto.TransactionDto;
import com.carddemo.common.entity.TransactionCategory;
import com.carddemo.common.entity.TransactionType;
import com.carddemo.transaction.dto.CreateTransactionRequest;
import com.carddemo.transaction.dto.TransactionSummaryDto;
import com.carddemo.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Transaction management endpoints")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    @Operation(summary = "Get all transactions", description = "Get paginated list of all transactions")
    public ResponseEntity<ApiResponse<PagedResponse<TransactionDto>>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "originationTimestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PagedResponse<TransactionDto> transactions = transactionService.getAllTransactions(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction by ID", description = "Get transaction details by transaction ID")
    public ResponseEntity<ApiResponse<TransactionDto>> getTransactionById(@PathVariable String transactionId) {
        TransactionDto transaction = transactionService.getTransactionById(transactionId);
        return ResponseEntity.ok(ApiResponse.success(transaction));
    }

    @GetMapping("/card/{cardNumber}")
    @Operation(summary = "Get transactions by card", description = "Get all transactions for a card")
    public ResponseEntity<ApiResponse<PagedResponse<TransactionDto>>> getTransactionsByCardNumber(
            @PathVariable String cardNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponse<TransactionDto> transactions = transactionService.getTransactionsByCardNumber(cardNumber, page, size);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/card/{cardNumber}/date-range")
    @Operation(summary = "Get transactions by card and date range", description = "Get transactions for a card within a date range")
    public ResponseEntity<ApiResponse<List<TransactionDto>>> getTransactionsByCardAndDateRange(
            @PathVariable String cardNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<TransactionDto> transactions = transactionService.getTransactionsByCardAndDateRange(cardNumber, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get transactions by date range", description = "Get all transactions within a date range")
    public ResponseEntity<ApiResponse<PagedResponse<TransactionDto>>> getTransactionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponse<TransactionDto> transactions = transactionService.getTransactionsByDateRange(startDate, endDate, page, size);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @PostMapping
    @Operation(summary = "Create transaction", description = "Create a new transaction")
    public ResponseEntity<ApiResponse<TransactionDto>> createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        TransactionDto transaction = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transaction created successfully", transaction));
    }

    @GetMapping("/type/{transactionTypeCode}")
    @Operation(summary = "Get transactions by type", description = "Get all transactions of a specific type")
    public ResponseEntity<ApiResponse<List<TransactionDto>>> getTransactionsByType(@PathVariable String transactionTypeCode) {
        List<TransactionDto> transactions = transactionService.getTransactionsByType(transactionTypeCode);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/merchant/{merchantId}")
    @Operation(summary = "Get transactions by merchant", description = "Get all transactions for a merchant")
    public ResponseEntity<ApiResponse<List<TransactionDto>>> getTransactionsByMerchant(@PathVariable Long merchantId) {
        List<TransactionDto> transactions = transactionService.getTransactionsByMerchant(merchantId);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get transaction summary", description = "Get summary statistics for transactions")
    public ResponseEntity<ApiResponse<TransactionSummaryDto>> getTransactionSummary() {
        TransactionSummaryDto summary = transactionService.getTransactionSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/types")
    @Operation(summary = "Get transaction types", description = "Get all transaction types")
    public ResponseEntity<ApiResponse<List<TransactionType>>> getAllTransactionTypes() {
        List<TransactionType> types = transactionService.getAllTransactionTypes();
        return ResponseEntity.ok(ApiResponse.success(types));
    }

    @GetMapping("/categories/{transactionTypeCode}")
    @Operation(summary = "Get transaction categories", description = "Get transaction categories for a type")
    public ResponseEntity<ApiResponse<List<TransactionCategory>>> getTransactionCategories(@PathVariable String transactionTypeCode) {
        List<TransactionCategory> categories = transactionService.getTransactionCategories(transactionTypeCode);
        return ResponseEntity.ok(ApiResponse.success(categories));
    }
}
