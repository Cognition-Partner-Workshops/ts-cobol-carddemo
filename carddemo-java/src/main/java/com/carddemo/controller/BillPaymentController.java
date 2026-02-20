package com.carddemo.controller;

import com.carddemo.dto.BillPaymentRequest;
import com.carddemo.entity.Transaction;
import com.carddemo.service.BillPaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bill-payments")
public class BillPaymentController {

    private final BillPaymentService billPaymentService;

    public BillPaymentController(BillPaymentService billPaymentService) {
        this.billPaymentService = billPaymentService;
    }

    @PostMapping
    public ResponseEntity<Transaction> processBillPayment(
            @Valid @RequestBody BillPaymentRequest request) {
        return ResponseEntity.ok(billPaymentService.processBillPayment(request));
    }
}
