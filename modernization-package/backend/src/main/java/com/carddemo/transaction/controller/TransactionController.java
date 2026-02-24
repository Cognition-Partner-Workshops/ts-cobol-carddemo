package com.carddemo.transaction.controller;

import com.carddemo.transaction.dto.*;
import com.carddemo.transaction.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Transaction Processing endpoints.
 * Replaces legacy CICS programs COTRN00C (List), COTRN01C (View), COTRN02C (Add).
 *
 * Endpoints:
 * - GET  /api/v1/transactions              -> CT00 List (paginated)
 * - GET  /api/v1/transactions/latest        -> PF5 Copy Last
 * - GET  /api/v1/transactions/{id}          -> CT01 View
 * - POST /api/v1/transactions               -> CT02 Add (6-phase validation)
 */
@RestController
@RequestMapping("/api/v1")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * CT00 - List Transactions with pagination (BR-LT-01 through BR-LT-08).
     * PF7 (Previous) = page-1, PF8 (Next) = page+1, ENTER with filter = startTransactionId.
     */
    @GetMapping("/transactions")
    public ResponseEntity<TransactionListResponse> listTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String startTransactionId) {
        TransactionListResponse response = transactionService.listTransactions(page, size, startTransactionId);
        return ResponseEntity.ok(response);
    }

    /**
     * PF5 - Get Latest Transaction (Copy Last) for CT02 Add screen.
     * Must be mapped BEFORE /transactions/{transactionId} to avoid path conflict.
     */
    @GetMapping("/transactions/latest")
    public ResponseEntity<LatestTransactionResponse> getLatestTransaction() {
        LatestTransactionResponse response = transactionService.getLatestTransaction();
        return ResponseEntity.ok(response);
    }

    /**
     * CT01 - View Transaction detail with all 13 fields (BR-VT-01 through BR-VT-05).
     * Read-only: no PUT/PATCH endpoint exists (BR-VT-04).
     */
    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<TransactionDetailResponse> viewTransaction(
            @PathVariable String transactionId) {
        TransactionDetailResponse response = transactionService.viewTransaction(transactionId);
        return ResponseEntity.ok(response);
    }

    /**
     * CT02 - Add Transaction with full 6-phase validation chain (BR-AT-01 through BR-AT-14).
     *
     * Response codes:
     * - 201: Transaction created successfully
     * - 200: Confirmation required (validation passed, awaiting Y/N)
     * - 400: Validation error from 6-phase chain
     * - 404: Cross-reference not found
     * - 409: Duplicate Transaction ID
     */
    @PostMapping("/transactions")
    public ResponseEntity<?> addTransaction(@RequestBody AddTransactionRequest request) {
        Object result = transactionService.addTransaction(request);

        if (result instanceof AddTransactionResponse addResponse) {
            return ResponseEntity.status(HttpStatus.CREATED).body(addResponse);
        } else if (result instanceof ConfirmationRequiredResponse confirmResponse) {
            return ResponseEntity.ok(confirmResponse);
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
