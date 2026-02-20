package com.carddemo.controller;

import com.carddemo.entity.TransactionCategory;
import com.carddemo.entity.TransactionType;
import com.carddemo.service.TransactionTypeService;
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
    public ResponseEntity<List<TransactionType>> listTypes() {
        return ResponseEntity.ok(transactionTypeService.listTransactionTypes());
    }

    @GetMapping("/{typeCd}")
    public ResponseEntity<TransactionType> getType(@PathVariable String typeCd) {
        return ResponseEntity.ok(transactionTypeService.getTransactionType(typeCd));
    }

    @PostMapping
    public ResponseEntity<TransactionType> createType(@RequestBody TransactionType type) {
        return ResponseEntity.ok(transactionTypeService.createTransactionType(type));
    }

    @PutMapping("/{typeCd}")
    public ResponseEntity<TransactionType> updateType(@PathVariable String typeCd,
                                                      @RequestBody TransactionType type) {
        return ResponseEntity.ok(transactionTypeService.updateTransactionType(typeCd, type));
    }

    @DeleteMapping("/{typeCd}")
    public ResponseEntity<Void> deleteType(@PathVariable String typeCd) {
        transactionTypeService.deleteTransactionType(typeCd);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{typeCd}/categories")
    public ResponseEntity<List<TransactionCategory>> listCategories(@PathVariable String typeCd) {
        return ResponseEntity.ok(transactionTypeService.listCategoriesByType(typeCd));
    }

    @PostMapping("/{typeCd}/categories")
    public ResponseEntity<TransactionCategory> createCategory(
            @PathVariable String typeCd, @RequestBody TransactionCategory category) {
        category.setTypeCd(typeCd);
        return ResponseEntity.ok(transactionTypeService.createCategory(category));
    }

    @PutMapping("/{typeCd}/categories/{catCd}")
    public ResponseEntity<TransactionCategory> updateCategory(
            @PathVariable String typeCd, @PathVariable Integer catCd,
            @RequestBody TransactionCategory category) {
        return ResponseEntity.ok(transactionTypeService.updateCategory(typeCd, catCd, category));
    }

    @DeleteMapping("/{typeCd}/categories/{catCd}")
    public ResponseEntity<Void> deleteCategory(@PathVariable String typeCd,
                                               @PathVariable Integer catCd) {
        transactionTypeService.deleteCategory(typeCd, catCd);
        return ResponseEntity.noContent().build();
    }
}
