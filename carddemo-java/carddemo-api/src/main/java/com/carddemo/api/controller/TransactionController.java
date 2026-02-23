package com.carddemo.api.controller;

import com.carddemo.api.dto.PageResponse;
import com.carddemo.api.dto.TransactionCreateRequest;
import com.carddemo.api.dto.TransactionResponse;
import com.carddemo.api.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Transaction REST controller.
 * Replaces CICS transactions CT00 (COTRN00C), CT01 (COTRN01C), and CT02 (COTRN02C).
 *
 * COBOL → Java mapping:
 *   CT00 → GET  /api/transactions              (Transaction List)
 *   CT01 → GET  /api/transactions/{id}          (Transaction View)
 *   CT02 → POST /api/transactions               (Transaction Add)
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction management (replaces CICS CT00/CT01/CT02)")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    @Operation(summary = "List transactions", description = "Paginated transaction listing with filters")
    public ResponseEntity<PageResponse<TransactionResponse>> listTransactions(
            @Parameter(description = "Filter by card number") @RequestParam(required = false) String cardNumber,
            @Parameter(description = "Filter by account ID") @RequestParam(required = false) Long accountId,
            @Parameter(description = "Start date filter") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date filter") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(transactionService.listTransactions(
                cardNumber, accountId, startDate, endDate,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "origTimestamp"))));
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "View transaction", description = "Retrieves transaction by ID (replaces CT01)")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable String transactionId) {
        return ResponseEntity.ok(transactionService.getTransaction(transactionId));
    }

    @PostMapping
    @Operation(summary = "Create transaction", description = "Creates a new transaction (replaces CT02)")
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody TransactionCreateRequest request) {
        TransactionResponse response = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
