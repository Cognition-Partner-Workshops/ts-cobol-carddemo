package com.carddemo.api.controller;

import com.carddemo.api.dto.PaymentRequest;
import com.carddemo.api.dto.TransactionResponse;
import com.carddemo.api.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payment REST controller.
 * Replaces CICS transaction CB00 (COBIL00C - Bill Payment).
 *
 * COBOL → Java mapping:
 *   CB00 → POST /api/payments (Bill Payment)
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Bill payment processing (replaces CICS CB00)")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Process bill payment", description = "Processes a bill payment (replaces CB00)")
    public ResponseEntity<TransactionResponse> processPayment(
            @Valid @RequestBody PaymentRequest request) {
        TransactionResponse response = paymentService.processPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
