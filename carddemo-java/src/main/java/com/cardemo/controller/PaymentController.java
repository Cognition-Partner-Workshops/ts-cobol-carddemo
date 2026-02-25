package com.cardemo.controller;

import com.cardemo.dto.PaymentRequest;
import com.cardemo.entity.Transaction;
import com.cardemo.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payment controller.
 * Migrated from CB00 transaction / COBIL00C program.
 */
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * POST /payments - Migrated from CB00 (COBIL00C) bill payment screen.
     */
    @PostMapping
    public ResponseEntity<Transaction> processPayment(@Valid @RequestBody PaymentRequest request) {
        Transaction payment = paymentService.processPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }
}
