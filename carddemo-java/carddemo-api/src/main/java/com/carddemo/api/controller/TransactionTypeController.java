package com.carddemo.api.controller;

import com.carddemo.api.dto.TransactionTypeRequest;
import com.carddemo.api.dto.TransactionTypeResponse;
import com.carddemo.api.service.TransactionTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Transaction Type administration REST controller.
 * Replaces CICS transactions CTTU (COTRTUPC) and CTLI (COTRTLIC) from the DB2 module.
 *
 * COBOL → Java mapping:
 *   CTLI → GET    /api/admin/transaction-types              (List Types)
 *   CTTU → POST   /api/admin/transaction-types              (Create Type)
 *   CTTU → PUT    /api/admin/transaction-types/{typeCode}   (Update Type)
 *   CTTU → DELETE /api/admin/transaction-types/{typeCode}   (Delete Type)
 */
@RestController
@RequestMapping("/api/admin/transaction-types")
@RequiredArgsConstructor
@Tag(name = "Transaction Type Administration",
        description = "Transaction type management (replaces CICS CTTU/CTLI)")
public class TransactionTypeController {

    private final TransactionTypeService transactionTypeService;

    @GetMapping
    @Operation(summary = "List transaction types", description = "Returns all transaction types (replaces CTLI)")
    public ResponseEntity<List<TransactionTypeResponse>> listTransactionTypes() {
        return ResponseEntity.ok(transactionTypeService.listTransactionTypes());
    }

    @PostMapping
    @Operation(summary = "Create transaction type", description = "Creates a new transaction type")
    public ResponseEntity<TransactionTypeResponse> createTransactionType(
            @Valid @RequestBody TransactionTypeRequest request) {
        TransactionTypeResponse response = transactionTypeService.createTransactionType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{typeCode}")
    @Operation(summary = "Update transaction type", description = "Updates an existing transaction type")
    public ResponseEntity<TransactionTypeResponse> updateTransactionType(
            @PathVariable String typeCode,
            @Valid @RequestBody TransactionTypeRequest request) {
        return ResponseEntity.ok(transactionTypeService.updateTransactionType(typeCode, request));
    }

    @DeleteMapping("/{typeCode}")
    @Operation(summary = "Delete transaction type", description = "Deletes a transaction type")
    public ResponseEntity<Void> deleteTransactionType(@PathVariable String typeCode) {
        transactionTypeService.deleteTransactionType(typeCode);
        return ResponseEntity.noContent().build();
    }
}
