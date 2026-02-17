package com.aws.carddemo.transaction;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aws.carddemo.transaction.dto.TransactionTypeResponse;

@RestController
@RequestMapping("/api/transaction-types")
public class TransactionTypeController {

    private final TransactionTypeService transactionTypeService;

    public TransactionTypeController(TransactionTypeService transactionTypeService) {
        this.transactionTypeService = transactionTypeService;
    }

    @GetMapping
    public ResponseEntity<List<TransactionTypeResponse>> listTypes() {
        return ResponseEntity.ok(transactionTypeService.listAll());
    }

    @GetMapping("/{typeCode}")
    public ResponseEntity<TransactionTypeResponse> getType(@PathVariable String typeCode) {
        return ResponseEntity.ok(transactionTypeService.getByCode(typeCode));
    }
}
