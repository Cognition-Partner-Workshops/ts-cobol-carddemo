package com.carddemo.transactiontype.controller;

import com.carddemo.transactiontype.dto.TransactionTypeDto;
import com.carddemo.transactiontype.dto.TransactionCategoryDto;
import com.carddemo.transactiontype.entity.TransactionType;
import com.carddemo.transactiontype.entity.TransactionCategory;
import com.carddemo.transactiontype.entity.TransactionCategoryBalance;
import com.carddemo.transactiontype.repository.TransactionCategoryBalanceRepository;
import com.carddemo.transactiontype.service.TransactionTypeService;
import com.carddemo.transactiontype.service.TransactionCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transaction-types")
@RequiredArgsConstructor
@Slf4j
public class TransactionTypeController {
    
    private final TransactionTypeService typeService;
    private final TransactionCategoryService categoryService;
    private final TransactionCategoryBalanceRepository balanceRepository;
    
    // EPIC-010 Feature 1: Transaction Type Management
    @GetMapping("/types")
    public ResponseEntity<List<TransactionType>> getAllTypes() {
        List<TransactionType> types = typeService.getAllTypes();
        return ResponseEntity.ok(types);
    }
    
    @GetMapping("/types/active")
    public ResponseEntity<List<TransactionType>> getActiveTypes() {
        List<TransactionType> types = typeService.getActiveTypes();
        return ResponseEntity.ok(types);
    }
    
    @GetMapping("/types/{typeCode}")
    public ResponseEntity<TransactionType> getType(@PathVariable String typeCode) {
        return typeService.getTypeByCode(typeCode)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/types/category/{categoryCode}")
    public ResponseEntity<List<TransactionType>> getTypesByCategory(@PathVariable String categoryCode) {
        List<TransactionType> types = typeService.getTypesByCategory(categoryCode);
        return ResponseEntity.ok(types);
    }
    
    @GetMapping("/types/debit")
    public ResponseEntity<List<TransactionType>> getDebitTypes() {
        List<TransactionType> types = typeService.getDebitTypes();
        return ResponseEntity.ok(types);
    }
    
    @GetMapping("/types/credit")
    public ResponseEntity<List<TransactionType>> getCreditTypes() {
        List<TransactionType> types = typeService.getCreditTypes();
        return ResponseEntity.ok(types);
    }
    
    @PostMapping("/types")
    public ResponseEntity<TransactionType> createType(@RequestBody TransactionTypeDto dto) {
        TransactionType type = typeService.createType(dto);
        return ResponseEntity.ok(type);
    }
    
    @PutMapping("/types/{typeCode}")
    public ResponseEntity<TransactionType> updateType(
            @PathVariable String typeCode,
            @RequestBody TransactionTypeDto dto) {
        return typeService.updateType(typeCode, dto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/types/{typeCode}")
    public ResponseEntity<Void> deleteType(@PathVariable String typeCode) {
        if (typeService.deleteType(typeCode)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
    
    @PostMapping("/types/{typeCode}/toggle")
    public ResponseEntity<TransactionType> toggleTypeStatus(@PathVariable String typeCode) {
        return typeService.toggleTypeStatus(typeCode)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // EPIC-010 Feature 2: Transaction Category Management
    @GetMapping("/categories")
    public ResponseEntity<List<TransactionCategory>> getAllCategories() {
        List<TransactionCategory> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }
    
    @GetMapping("/categories/active")
    public ResponseEntity<List<TransactionCategory>> getActiveCategories() {
        List<TransactionCategory> categories = categoryService.getActiveCategories();
        return ResponseEntity.ok(categories);
    }
    
    @GetMapping("/categories/{categoryCode}")
    public ResponseEntity<TransactionCategory> getCategory(@PathVariable String categoryCode) {
        return categoryService.getCategoryByCode(categoryCode)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/categories/root")
    public ResponseEntity<List<TransactionCategory>> getRootCategories() {
        List<TransactionCategory> categories = categoryService.getRootCategories();
        return ResponseEntity.ok(categories);
    }
    
    @GetMapping("/categories/{parentCode}/children")
    public ResponseEntity<List<TransactionCategory>> getChildCategories(@PathVariable String parentCode) {
        List<TransactionCategory> categories = categoryService.getChildCategories(parentCode);
        return ResponseEntity.ok(categories);
    }
    
    @GetMapping("/categories/type/{categoryType}")
    public ResponseEntity<List<TransactionCategory>> getCategoriesByType(@PathVariable String categoryType) {
        List<TransactionCategory> categories = categoryService.getCategoriesByType(categoryType);
        return ResponseEntity.ok(categories);
    }
    
    @GetMapping("/categories/reporting-group/{reportingGroup}")
    public ResponseEntity<List<TransactionCategory>> getCategoriesByReportingGroup(@PathVariable String reportingGroup) {
        List<TransactionCategory> categories = categoryService.getCategoriesByReportingGroup(reportingGroup);
        return ResponseEntity.ok(categories);
    }
    
    @PostMapping("/categories")
    public ResponseEntity<TransactionCategory> createCategory(@RequestBody TransactionCategoryDto dto) {
        TransactionCategory category = categoryService.createCategory(dto);
        return ResponseEntity.ok(category);
    }
    
    @PutMapping("/categories/{categoryCode}")
    public ResponseEntity<TransactionCategory> updateCategory(
            @PathVariable String categoryCode,
            @RequestBody TransactionCategoryDto dto) {
        return categoryService.updateCategory(categoryCode, dto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/categories/{categoryCode}")
    public ResponseEntity<Void> deleteCategory(@PathVariable String categoryCode) {
        if (categoryService.deleteCategory(categoryCode)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
    
    @PostMapping("/categories/{categoryCode}/toggle")
    public ResponseEntity<TransactionCategory> toggleCategoryStatus(@PathVariable String categoryCode) {
        return categoryService.toggleCategoryStatus(categoryCode)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // EPIC-010 Feature 3: Category Balance Tracking
    @GetMapping("/balances/account/{accountId}")
    public ResponseEntity<List<TransactionCategoryBalance>> getBalancesByAccount(@PathVariable String accountId) {
        List<TransactionCategoryBalance> balances = balanceRepository.findByAccountIdOrderByDateDesc(accountId);
        return ResponseEntity.ok(balances);
    }
    
    @GetMapping("/balances/account/{accountId}/range")
    public ResponseEntity<List<TransactionCategoryBalance>> getBalancesByAccountAndDateRange(
            @PathVariable String accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<TransactionCategoryBalance> balances = balanceRepository.findByAccountIdAndDateRange(accountId, startDate, endDate);
        return ResponseEntity.ok(balances);
    }
    
    @GetMapping("/balances/category/{categoryCode}")
    public ResponseEntity<List<TransactionCategoryBalance>> getBalancesByCategory(@PathVariable String categoryCode) {
        List<TransactionCategoryBalance> balances = balanceRepository.findByCategoryCode(categoryCode);
        return ResponseEntity.ok(balances);
    }
}
