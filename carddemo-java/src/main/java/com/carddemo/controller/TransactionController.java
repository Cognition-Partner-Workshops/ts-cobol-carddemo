package com.carddemo.controller;

import com.carddemo.dto.PaymentRequest;
import com.carddemo.dto.ReportRequest;
import com.carddemo.dto.TransactionDto;
import com.carddemo.entity.Transaction;
import com.carddemo.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/transactions/list")
    public ResponseEntity<Page<Transaction>> listTransactions(
            @RequestParam(required = false) String cardNum,
            @RequestParam(required = false) Long acctId,
            @PageableDefault(size = 10) Pageable pageable) {
        if (cardNum != null) {
            return ResponseEntity.ok(transactionService.listTransactionsByCard(cardNum, pageable));
        }
        if (acctId != null) {
            return ResponseEntity.ok(transactionService.listTransactionsByAccount(acctId, pageable));
        }
        return ResponseEntity.ok(transactionService.listTransactions(pageable));
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<Transaction> viewTransaction(@PathVariable("id") String tranId) {
        return ResponseEntity.ok(transactionService.getTransaction(tranId));
    }

    @PostMapping("/transactions")
    public ResponseEntity<Transaction> addTransaction(@Valid @RequestBody TransactionDto dto) {
        Transaction transaction = transactionService.addTransaction(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    @PostMapping("/reports")
    public ResponseEntity<List<Transaction>> getReport(@RequestBody ReportRequest request) {
        List<Transaction> transactions = transactionService.getTransactionReport(
                request.getCardNum(), request.getStartDate(), request.getEndDate());
        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/payments")
    public ResponseEntity<Transaction> makePayment(@Valid @RequestBody PaymentRequest request) {
        Transaction transaction = transactionService.processPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }
}
