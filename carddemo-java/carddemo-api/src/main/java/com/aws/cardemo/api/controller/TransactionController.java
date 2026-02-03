package com.aws.cardemo.api.controller;

import com.aws.cardemo.domain.entity.Transaction;
import com.aws.cardemo.services.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

/**
 * REST Controller for managing Transaction resources.
 * 
 * This controller provides endpoints for CRUD operations on credit card transactions,
 * as well as specialized queries for transaction history and reporting. It handles
 * all transaction-related operations in the CardDemo application, supporting the
 * modernized mainframe credit card transaction processing system.
 * 
 * All endpoints are prefixed with /api/v1/transactions and return JSON responses.
 * The controller delegates business logic to the TransactionService layer.
 * 
 * @author CardDemo Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction", description = "Transaction management APIs for credit card transaction operations")
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Retrieves all transactions from the system.
     * 
     * This endpoint returns a complete list of all credit card transactions stored in the database.
     * Use with caution in production environments as it may return a very large dataset.
     * Consider using the paginated endpoint for production use.
     * 
     * @return ResponseEntity containing a list of all Transaction entities with HTTP 200 status
     */
    @GetMapping
    @Operation(summary = "Get all transactions", 
               description = "Retrieves a complete list of all credit card transactions in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved all transactions")
    })
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    /**
     * Retrieves a specific transaction by its unique identifier.
     * 
     * This endpoint fetches a single transaction based on the provided transaction ID.
     * Returns HTTP 404 if the transaction is not found.
     * 
     * @param transactionId The unique identifier of the transaction to retrieve (16 characters max)
     * @return ResponseEntity containing the Transaction if found, or HTTP 404 if not found
     */
    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction by ID", 
               description = "Retrieves a specific transaction using its unique identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transaction found and returned successfully"),
        @ApiResponse(responseCode = "404", description = "Transaction not found with the given ID")
    })
    public ResponseEntity<Transaction> getTransactionById(
            @Parameter(description = "Unique transaction identifier", required = true)
            @PathVariable String transactionId) {
        return transactionService.getTransactionById(transactionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new credit card transaction.
     * 
     * This endpoint accepts transaction details in the request body and creates a new transaction
     * in the system. The transaction data is validated before creation, including the card number,
     * amount, and merchant information.
     * 
     * @param transaction The Transaction entity containing the details for the new transaction
     * @return ResponseEntity containing the created Transaction with HTTP 201 status
     */
    @PostMapping
    @Operation(summary = "Create a new transaction", 
               description = "Creates a new credit card transaction with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Transaction created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid transaction data provided")
    })
    public ResponseEntity<Transaction> createTransaction(
            @Parameter(description = "Transaction details for creation", required = true)
            @Valid @RequestBody Transaction transaction) {
        Transaction created = transactionService.createTransaction(transaction);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Updates an existing credit card transaction.
     * 
     * This endpoint updates the transaction identified by the transactionId path parameter
     * with the data provided in the request body. The transaction ID in the path takes
     * precedence over any ID in the request body. Note: In production, transactions are
     * typically immutable; this endpoint is provided for administrative corrections.
     * 
     * @param transactionId The unique identifier of the transaction to update
     * @param transaction The Transaction entity containing the updated details
     * @return ResponseEntity containing the updated Transaction with HTTP 200 status
     */
    @PutMapping("/{transactionId}")
    @Operation(summary = "Update an existing transaction", 
               description = "Updates an existing transaction with the provided details (admin use)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transaction updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid transaction data provided"),
        @ApiResponse(responseCode = "404", description = "Transaction not found with the given ID")
    })
    public ResponseEntity<Transaction> updateTransaction(
            @Parameter(description = "Unique transaction identifier", required = true)
            @PathVariable String transactionId,
            @Parameter(description = "Updated transaction details", required = true)
            @Valid @RequestBody Transaction transaction) {
        transaction.setTransactionId(transactionId);
        Transaction updated = transactionService.updateTransaction(transaction);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a credit card transaction from the system.
     * 
     * This endpoint permanently removes the transaction identified by the transactionId.
     * This operation cannot be undone. In production, consider implementing soft delete
     * or archiving for audit trail purposes.
     * 
     * @param transactionId The unique identifier of the transaction to delete
     * @return ResponseEntity with HTTP 204 (No Content) status on successful deletion
     */
    @DeleteMapping("/{transactionId}")
    @Operation(summary = "Delete a transaction", 
               description = "Permanently removes a transaction from the system (admin use)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Transaction deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Transaction not found with the given ID")
    })
    public ResponseEntity<Void> deleteTransaction(
            @Parameter(description = "Unique transaction identifier", required = true)
            @PathVariable String transactionId) {
        transactionService.deleteTransaction(transactionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves all transactions for a specific card.
     * 
     * This endpoint returns all transactions associated with the specified card number.
     * Useful for viewing complete transaction history for a card.
     * 
     * @param cardNumber The 16-digit card number to filter transactions by
     * @return ResponseEntity containing a list of transactions for the specified card
     */
    @GetMapping("/card/{cardNumber}")
    @Operation(summary = "Get transactions by card number", 
               description = "Retrieves all transactions for a specific credit card")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved transactions for card")
    })
    public ResponseEntity<List<Transaction>> getTransactionsByCardNumber(
            @Parameter(description = "16-digit card number", required = true)
            @PathVariable String cardNumber) {
        return ResponseEntity.ok(transactionService.getTransactionsByCardNumber(cardNumber));
    }

    /**
     * Retrieves transactions for a specific card with pagination support.
     * 
     * This endpoint returns paginated transactions for the specified card number.
     * Recommended for cards with large transaction histories to improve performance.
     * 
     * @param cardNumber The 16-digit card number to filter transactions by
     * @param pageable Pagination parameters (page, size, sort)
     * @return ResponseEntity containing a Page of transactions for the specified card
     */
    @GetMapping("/card/{cardNumber}/paged")
    @Operation(summary = "Get transactions by card number with pagination", 
               description = "Retrieves paginated transactions for a specific credit card")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated transactions")
    })
    public ResponseEntity<Page<Transaction>> getTransactionsByCardNumberPaged(
            @Parameter(description = "16-digit card number", required = true)
            @PathVariable String cardNumber,
            @Parameter(description = "Pagination parameters")
            Pageable pageable) {
        return ResponseEntity.ok(transactionService.getTransactionsByCardNumber(cardNumber, pageable));
    }

    /**
     * Retrieves transactions for a specific card within a date range.
     * 
     * This endpoint returns transactions for the specified card number that occurred
     * between the start and end dates (inclusive). Useful for statement generation
     * and period-specific reporting.
     * 
     * @param cardNumber The 16-digit card number to filter transactions by
     * @param startDate The start of the date range (ISO 8601 format)
     * @param endDate The end of the date range (ISO 8601 format)
     * @return ResponseEntity containing a list of transactions within the date range
     */
    @GetMapping("/card/{cardNumber}/date-range")
    @Operation(summary = "Get transactions by card number and date range", 
               description = "Retrieves transactions for a card within a specified date range")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved transactions in date range")
    })
    public ResponseEntity<List<Transaction>> getTransactionsByCardNumberAndDateRange(
            @Parameter(description = "16-digit card number", required = true)
            @PathVariable String cardNumber,
            @Parameter(description = "Start date in ISO 8601 format (e.g., 2024-01-01T00:00:00)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date in ISO 8601 format (e.g., 2024-01-31T23:59:59)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(
                transactionService.getTransactionsByCardNumberAndDateRange(cardNumber, startDate, endDate));
    }

    /**
     * Retrieves all transactions for a specific merchant.
     * 
     * This endpoint returns all transactions associated with the specified merchant ID.
     * Useful for merchant reconciliation and reporting purposes.
     * 
     * @param merchantId The merchant identifier to filter transactions by
     * @return ResponseEntity containing a list of transactions for the specified merchant
     */
    @GetMapping("/merchant/{merchantId}")
    @Operation(summary = "Get transactions by merchant ID", 
               description = "Retrieves all transactions for a specific merchant")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved transactions for merchant")
    })
    public ResponseEntity<List<Transaction>> getTransactionsByMerchantId(
            @Parameter(description = "Merchant identifier", required = true)
            @PathVariable String merchantId) {
        return ResponseEntity.ok(transactionService.getTransactionsByMerchantId(merchantId));
    }

    /**
     * Retrieves transactions from the last 24 hours.
     * 
     * This endpoint returns all transactions that occurred within the last 24 hours.
     * Useful for real-time monitoring, fraud detection, and daily activity reports.
     * 
     * @return ResponseEntity containing a list of recent transactions
     */
    @GetMapping("/recent")
    @Operation(summary = "Get transactions from the last 24 hours", 
               description = "Retrieves all transactions that occurred in the last 24 hours")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved recent transactions")
    })
    public ResponseEntity<List<Transaction>> getRecentTransactions() {
        return ResponseEntity.ok(transactionService.getTransactionsLast24Hours());
    }
}
