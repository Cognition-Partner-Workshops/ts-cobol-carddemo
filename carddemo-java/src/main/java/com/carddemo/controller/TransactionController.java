package com.carddemo.controller;

import com.carddemo.dto.TransactionReportEntry;
import com.carddemo.dto.TransactionRequest;
import com.carddemo.entity.Transaction;
import com.carddemo.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<Page<Transaction>> listTransactions(
            @RequestParam(required = false) String cardNum,
            @RequestParam(required = false) Long acctId,
            Pageable pageable) {
        if (cardNum != null) {
            return ResponseEntity.ok(transactionService.listTransactionsByCard(cardNum, pageable));
        }
        if (acctId != null) {
            return ResponseEntity.ok(transactionService.listTransactionsByAccount(acctId, pageable));
        }
        return ResponseEntity.ok(transactionService.listTransactions(pageable));
    }

    @GetMapping("/{tranId}")
    public ResponseEntity<Transaction> getTransaction(@PathVariable String tranId) {
        return ResponseEntity.ok(transactionService.getTransaction(tranId));
    }

    @PostMapping
    public ResponseEntity<Transaction> addTransaction(
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.addTransaction(request));
    }

    @GetMapping("/reports")
    public ResponseEntity<List<TransactionReportEntry>> getTransactionReport(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return ResponseEntity.ok(transactionService.generateTransactionReport(startDate, endDate));
    }
}
