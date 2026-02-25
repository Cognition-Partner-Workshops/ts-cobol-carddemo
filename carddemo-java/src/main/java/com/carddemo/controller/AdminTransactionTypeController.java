package com.carddemo.controller;

import com.carddemo.entity.TransactionCategory;
import com.carddemo.entity.TransactionType;
import com.carddemo.service.TransactionTypeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin Transaction Type controller - migrated from Phase 5a DB2 Transaction Type module:
 *   COTRTUPC (CTTU - Transaction Type Add/Edit)
 *   COTRTLIC (CTLI - Transaction Type List with forward/backward paging)
 *
 * COTRTLIC uses DB2 cursors for forward AND backward paging -
 * replaced with Spring Data JPA Pageable which supports both directions.
 *
 * All endpoints require ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin/transaction-types")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTransactionTypeController {

    private final TransactionTypeService typeService;

    public AdminTransactionTypeController(TransactionTypeService typeService) {
        this.typeService = typeService;
    }

    /**
     * GET /api/admin/transaction-types - List transaction types (CTLI).
     * Replaces COTRTLIC DB2 cursor-based paginated list.
     */
    @GetMapping
    public ResponseEntity<Page<TransactionType>> listTypes(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(typeService.listTypes(pageable));
    }

    /**
     * GET /api/admin/transaction-types/{typeCd} - Get type detail.
     */
    @GetMapping("/{typeCd}")
    public ResponseEntity<TransactionType> getType(@PathVariable String typeCd) {
        TransactionType type = typeService.getType(typeCd)
                .orElseThrow(() -> new IllegalArgumentException("Transaction type not found"));
        return ResponseEntity.ok(type);
    }

    /**
     * POST /api/admin/transaction-types - Add new type (CTTU add mode).
     * Replaces COTRTUPC INSERT INTO CARDDEMO.TRANSACTION_TYPE.
     */
    @PostMapping
    public ResponseEntity<TransactionType> addType(@RequestBody TransactionType type) {
        return ResponseEntity.status(HttpStatus.CREATED).body(typeService.addType(type));
    }

    /**
     * PUT /api/admin/transaction-types/{typeCd} - Edit type (CTTU edit mode).
     * Replaces COTRTUPC UPDATE CARDDEMO.TRANSACTION_TYPE.
     */
    @PutMapping("/{typeCd}")
    public ResponseEntity<TransactionType> updateType(
            @PathVariable String typeCd, @RequestBody TransactionType updatedData) {
        return ResponseEntity.ok(typeService.updateType(typeCd, updatedData));
    }

    /**
     * DELETE /api/admin/transaction-types/{typeCd} - Delete type.
     * Enforces DELETE RESTRICT if categories exist.
     */
    @DeleteMapping("/{typeCd}")
    public ResponseEntity<Void> deleteType(@PathVariable String typeCd) {
        typeService.deleteType(typeCd);
        return ResponseEntity.noContent().build();
    }

    // --- Transaction Category endpoints ---

    /**
     * GET /api/admin/transaction-types/{typeCd}/categories - List categories for a type.
     */
    @GetMapping("/{typeCd}/categories")
    public ResponseEntity<Page<TransactionCategory>> listCategories(
            @PathVariable String typeCd,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(typeService.listCategories(typeCd, pageable));
    }

    /**
     * POST /api/admin/transaction-types/{typeCd}/categories - Add category.
     */
    @PostMapping("/{typeCd}/categories")
    public ResponseEntity<TransactionCategory> addCategory(
            @PathVariable String typeCd, @RequestBody TransactionCategory category) {
        category.setTypeCd(typeCd);
        return ResponseEntity.status(HttpStatus.CREATED).body(typeService.addCategory(category));
    }

    /**
     * PUT /api/admin/transaction-types/{typeCd}/categories/{catCd} - Update category.
     */
    @PutMapping("/{typeCd}/categories/{catCd}")
    public ResponseEntity<TransactionCategory> updateCategory(
            @PathVariable String typeCd, @PathVariable Integer catCd,
            @RequestBody TransactionCategory updatedData) {
        return ResponseEntity.ok(typeService.updateCategory(typeCd, catCd, updatedData));
    }

    /**
     * DELETE /api/admin/transaction-types/{typeCd}/categories/{catCd} - Delete category.
     */
    @DeleteMapping("/{typeCd}/categories/{catCd}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable String typeCd, @PathVariable Integer catCd) {
        typeService.deleteCategory(typeCd, catCd);
        return ResponseEntity.noContent().build();
    }
}
