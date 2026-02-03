package com.carddemo.controller;

import com.carddemo.dto.ApiResponse;
import com.carddemo.dto.BillPaymentRequest;
import com.carddemo.dto.TransactionRequest;
import com.carddemo.model.Transaction;
import com.carddemo.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    
    private final TransactionService transactionService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<Page<Transaction>>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactions = transactionService.getAllTransactions(pageable);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }
    
    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<Transaction>> getTransactionById(@PathVariable String transactionId) {
        Transaction transaction = transactionService.getTransactionById(transactionId);
        return ResponseEntity.ok(ApiResponse.success(transaction));
    }
    
    @GetMapping("/card/{cardNumber}")
    public ResponseEntity<ApiResponse<List<Transaction>>> getTransactionsByCardNumber(
            @PathVariable String cardNumber) {
        List<Transaction> transactions = transactionService.getTransactionsByCardNumber(cardNumber);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }
    
    @GetMapping("/card/{cardNumber}/paged")
    public ResponseEntity<ApiResponse<Page<Transaction>>> getTransactionsByCardNumberPaged(
            @PathVariable String cardNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactions = transactionService.getTransactionsByCardNumber(cardNumber, pageable);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<Transaction>> createTransaction(
            @Valid @RequestBody TransactionRequest request) {
        Transaction transaction = transactionService.createTransaction(request);
        return ResponseEntity.ok(ApiResponse.success("Transaction created successfully", transaction));
    }
    
    @PostMapping("/bill-payment")
    public ResponseEntity<ApiResponse<Transaction>> processBillPayment(
            @Valid @RequestBody BillPaymentRequest request) {
        Transaction transaction = transactionService.processBillPayment(request);
        return ResponseEntity.ok(ApiResponse.success("Bill payment processed successfully", transaction));
    }
}
