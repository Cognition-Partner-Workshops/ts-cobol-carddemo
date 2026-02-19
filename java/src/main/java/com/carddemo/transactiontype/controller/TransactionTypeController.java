package com.carddemo.transactiontype.controller;

import com.carddemo.transactiontype.model.TransactionType;
import com.carddemo.transactiontype.service.TransactionTypeService;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/api/transaction-types")
public class TransactionTypeController {

    private final TransactionTypeService transactionTypeService;

    public TransactionTypeController(TransactionTypeService transactionTypeService) {
        this.transactionTypeService = transactionTypeService;
    }

    @GetMapping
    public ResponseEntity<List<TransactionType>> getAllTransactionTypes() {
        List<TransactionType> types = transactionTypeService.findAll();
        return ResponseEntity.ok(types);
    }

    @GetMapping("/{typeCode}")
    public ResponseEntity<TransactionType> getTransactionType(@PathVariable String typeCode) {
        TransactionType type = transactionTypeService.findByTypeCode(typeCode);
        return ResponseEntity.ok(type);
    }

    @PostMapping
    public ResponseEntity<TransactionType> createTransactionType(
            @Valid @RequestBody TransactionType transactionType) {
        TransactionType created = transactionTypeService.create(transactionType);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{typeCode}")
    public ResponseEntity<TransactionType> updateTransactionType(
            @PathVariable String typeCode,
            @Valid @RequestBody TransactionType transactionType) {
        TransactionType updated = transactionTypeService.update(typeCode, transactionType);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{typeCode}")
    public ResponseEntity<Void> deleteTransactionType(@PathVariable String typeCode) {
        transactionTypeService.delete(typeCode);
        return ResponseEntity.noContent().build();
    }
}
