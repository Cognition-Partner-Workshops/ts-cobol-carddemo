package com.cardemo.controller;

import com.cardemo.entity.TransactionTypeDb2;
import com.cardemo.entity.TransactionTypeCategoryDb2;
import com.cardemo.service.TransactionTypeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Transaction type management controller (Optional DB2 module, Admin only).
 * Migrated from CTTU (COTRTUPC - add/update) and CTLI (COTRTLIC - list).
 */
@RestController
@RequestMapping("/admin/transaction-types")
public class TransactionTypeController {

    private final TransactionTypeService transactionTypeService;

    public TransactionTypeController(TransactionTypeService transactionTypeService) {
        this.transactionTypeService = transactionTypeService;
    }

    /**
     * GET /admin/transaction-types - Migrated from CTLI (COTRTLIC) with cursor-based paging.
     */
    @GetMapping
    public ResponseEntity<Page<TransactionTypeDb2>> listTransactionTypes(Pageable pageable) {
        return ResponseEntity.ok(transactionTypeService.listTransactionTypes(pageable));
    }

    /**
     * POST /admin/transaction-types - Migrated from CTTU (COTRTUPC) add mode.
     */
    @PostMapping
    public ResponseEntity<TransactionTypeDb2> createTransactionType(@RequestBody TransactionTypeDb2 type) {
        TransactionTypeDb2 created = transactionTypeService.createTransactionType(type);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /admin/transaction-types/{code} - Migrated from CTTU (COTRTUPC) update mode.
     */
    @PutMapping("/{code}")
    public ResponseEntity<TransactionTypeDb2> updateTransactionType(
            @PathVariable("code") String code, @RequestBody TransactionTypeDb2 type) {
        return ResponseEntity.ok(transactionTypeService.updateTransactionType(code, type));
    }

    /**
     * GET /admin/transaction-types/{code}/categories - Get categories for a type.
     */
    @GetMapping("/{code}/categories")
    public ResponseEntity<List<TransactionTypeCategoryDb2>> getCategories(@PathVariable("code") String code) {
        return ResponseEntity.ok(transactionTypeService.getCategoriesByType(code));
    }
}
