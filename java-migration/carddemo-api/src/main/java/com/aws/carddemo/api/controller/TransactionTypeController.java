package com.aws.carddemo.api.controller;

import com.aws.carddemo.service.transactiontype.TransactionTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transaction-types")
@RequiredArgsConstructor
@Tag(name = "Transaction Types", description = "Transaction type management endpoints - migrated from COTRTUPC (CTTU)")
public class TransactionTypeController {

    private final TransactionTypeService transactionTypeService;

    @GetMapping
    @Operation(summary = "List all transaction types with pagination")
    public ResponseEntity<Page<TransactionTypeService.TransactionTypeDTO>> listTransactionTypes(Pageable pageable) {
        return ResponseEntity.ok(transactionTypeService.listTransactionTypes(pageable));
    }

    @GetMapping("/active")
    @Operation(summary = "List active transaction types")
    public ResponseEntity<List<TransactionTypeService.TransactionTypeDTO>> listActiveTransactionTypes() {
        return ResponseEntity.ok(transactionTypeService.listActiveTransactionTypes());
    }

    @GetMapping("/{typeCode}")
    @Operation(summary = "Get transaction type by code")
    public ResponseEntity<TransactionTypeService.TransactionTypeDTO> getTransactionType(@PathVariable String typeCode) {
        return transactionTypeService.getTransactionType(typeCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{categoryCode}")
    @Operation(summary = "Get transaction types by category")
    public ResponseEntity<List<TransactionTypeService.TransactionTypeDTO>> getByCategory(@PathVariable Integer categoryCode) {
        return ResponseEntity.ok(transactionTypeService.getByCategory(categoryCode));
    }

    @PostMapping
    @Operation(summary = "Create new transaction type")
    public ResponseEntity<TransactionTypeService.TransactionTypeDTO> createTransactionType(
            @Valid @RequestBody TransactionTypeService.TransactionTypeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionTypeService.createTransactionType(request));
    }

    @PutMapping("/{typeCode}")
    @Operation(summary = "Update transaction type")
    public ResponseEntity<TransactionTypeService.TransactionTypeDTO> updateTransactionType(
            @PathVariable String typeCode,
            @Valid @RequestBody TransactionTypeService.TransactionTypeUpdateRequest request) {
        return ResponseEntity.ok(transactionTypeService.updateTransactionType(typeCode, request));
    }

    @DeleteMapping("/{typeCode}")
    @Operation(summary = "Delete transaction type")
    public ResponseEntity<Void> deleteTransactionType(@PathVariable String typeCode) {
        transactionTypeService.deleteTransactionType(typeCode);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{typeCode}/deactivate")
    @Operation(summary = "Deactivate transaction type")
    public ResponseEntity<Void> deactivateTransactionType(@PathVariable String typeCode) {
        transactionTypeService.deactivateTransactionType(typeCode);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Search transaction types by description")
    public ResponseEntity<Page<TransactionTypeService.TransactionTypeDTO>> searchByDescription(
            @RequestParam String keyword,
            Pageable pageable) {
        return ResponseEntity.ok(transactionTypeService.searchByDescription(keyword, pageable));
    }
}
