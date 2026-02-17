package com.aws.carddemo.transaction;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aws.carddemo.transaction.dto.TransactionCategoryResponse;

@RestController
@RequestMapping("/api/transaction-categories")
public class TransactionCategoryController {

    private final TransactionCategoryService transactionCategoryService;

    public TransactionCategoryController(TransactionCategoryService transactionCategoryService) {
        this.transactionCategoryService = transactionCategoryService;
    }

    @GetMapping
    public ResponseEntity<List<TransactionCategoryResponse>> listCategories() {
        return ResponseEntity.ok(transactionCategoryService.listAll());
    }

    @GetMapping("/{catCode}")
    public ResponseEntity<TransactionCategoryResponse> getCategory(@PathVariable String catCode) {
        return ResponseEntity.ok(transactionCategoryService.getByCode(catCode));
    }
}
