package com.aws.carddemo.controller;

import com.aws.carddemo.dto.TransactionDto;
import com.aws.carddemo.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/{tranId}")
    public ResponseEntity<TransactionDto> getTransaction(@PathVariable String tranId) {
        return ResponseEntity.ok(transactionService.getTransaction(tranId));
    }

    @GetMapping
    public ResponseEntity<Page<TransactionDto>> getAllTransactions(Pageable pageable) {
        return ResponseEntity.ok(transactionService.getAllTransactions(pageable));
    }

    @GetMapping("/card/{cardNum}")
    public ResponseEntity<Page<TransactionDto>> getTransactionsByCard(@PathVariable String cardNum, Pageable pageable) {
        return ResponseEntity.ok(transactionService.getTransactionsByCard(cardNum, pageable));
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<TransactionDto>> getTransactionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(transactionService.getTransactionsByDateRange(startDate, endDate));
    }

    @GetMapping("/card/{cardNum}/date-range")
    public ResponseEntity<List<TransactionDto>> getTransactionsByCardAndDateRange(
            @PathVariable String cardNum,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(transactionService.getTransactionsByCardAndDateRange(cardNum, startDate, endDate));
    }

    @GetMapping("/large")
    public ResponseEntity<List<TransactionDto>> getLargeTransactions(@RequestParam BigDecimal amount) {
        return ResponseEntity.ok(transactionService.getLargeTransactions(amount));
    }

    @GetMapping("/card/{cardNum}/sum")
    public ResponseEntity<BigDecimal> sumTransactionsByCard(@PathVariable String cardNum) {
        return ResponseEntity.ok(transactionService.sumTransactionsByCard(cardNum));
    }

    @GetMapping("/account/{acctId}/sum")
    public ResponseEntity<BigDecimal> sumTransactionsByAccount(@PathVariable Long acctId) {
        return ResponseEntity.ok(transactionService.sumTransactionsByAccount(acctId));
    }

    @PostMapping
    public ResponseEntity<TransactionDto> createTransaction(@Valid @RequestBody TransactionDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.createTransaction(dto));
    }

    @PostMapping("/post")
    public ResponseEntity<TransactionDto> postTransaction(@Valid @RequestBody TransactionDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.postTransaction(dto));
    }

    @PostMapping("/validate")
    public ResponseEntity<Void> validateTransaction(@Valid @RequestBody TransactionDto dto) {
        transactionService.validateTransaction(dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/card/{cardNum}/count")
    public ResponseEntity<Long> countTransactionsByCard(@PathVariable String cardNum) {
        return ResponseEntity.ok(transactionService.countTransactionsByCard(cardNum));
    }
}
