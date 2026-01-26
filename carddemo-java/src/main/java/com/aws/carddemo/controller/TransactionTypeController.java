package com.aws.carddemo.controller;

import com.aws.carddemo.dto.TransactionTypeDto;
import com.aws.carddemo.service.TransactionTypeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transaction-types")
public class TransactionTypeController {

    private final TransactionTypeService transactionTypeService;

    public TransactionTypeController(TransactionTypeService transactionTypeService) {
        this.transactionTypeService = transactionTypeService;
    }

    @GetMapping("/{typeCode}")
    public ResponseEntity<TransactionTypeDto> getTransactionType(@PathVariable String typeCode) {
        return ResponseEntity.ok(transactionTypeService.getTransactionType(typeCode));
    }

    @GetMapping
    public ResponseEntity<List<TransactionTypeDto>> getAllTransactionTypes() {
        return ResponseEntity.ok(transactionTypeService.getAllTransactionTypes());
    }

    @GetMapping("/active")
    public ResponseEntity<List<TransactionTypeDto>> getActiveTransactionTypes() {
        return ResponseEntity.ok(transactionTypeService.getActiveTransactionTypes());
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<TransactionTypeDto>> getTransactionTypesByCategory(@PathVariable Integer categoryId) {
        return ResponseEntity.ok(transactionTypeService.getTransactionTypesByCategory(categoryId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransactionTypeDto> createTransactionType(@Valid @RequestBody TransactionTypeDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionTypeService.createTransactionType(dto));
    }

    @PutMapping("/{typeCode}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransactionTypeDto> updateTransactionType(@PathVariable String typeCode, 
                                                                     @Valid @RequestBody TransactionTypeDto dto) {
        return ResponseEntity.ok(transactionTypeService.updateTransactionType(typeCode, dto));
    }

    @PatchMapping("/{typeCode}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateTransactionType(@PathVariable String typeCode) {
        transactionTypeService.deactivateTransactionType(typeCode);
        return ResponseEntity.noContent().build();
    }
}
