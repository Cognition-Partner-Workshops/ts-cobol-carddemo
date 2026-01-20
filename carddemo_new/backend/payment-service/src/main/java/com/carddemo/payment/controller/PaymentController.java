package com.carddemo.payment.controller;

import com.carddemo.common.dto.ApiResponse;
import com.carddemo.common.dto.PageResponse;
import com.carddemo.payment.dto.PaymentDto;
import com.carddemo.payment.dto.PaymentRequest;
import com.carddemo.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Processing", description = "Bill payment operations - EPIC-005")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment details", description = "View payment details by ID")
    public ResponseEntity<ApiResponse<PaymentDto>> getPayment(@PathVariable String paymentId) {
        PaymentDto payment = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get payments by account", description = "List all payments for an account (US-005-01-01)")
    public ResponseEntity<ApiResponse<PageResponse<PaymentDto>>> getPaymentsByAccount(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<PaymentDto> payments = paymentService.getPaymentsByAccount(accountId, page, size);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @PostMapping
    @Operation(summary = "Create payment", description = "Submit a new bill payment (US-005-01-02, US-005-02-01)")
    public ResponseEntity<ApiResponse<PaymentDto>> createPayment(
            @Valid @RequestBody PaymentRequest request) {
        PaymentDto payment = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(payment, "Payment submitted successfully. Confirmation: " + payment.getConfirmationNumber()));
    }

    @PostMapping("/{paymentId}/process")
    @Operation(summary = "Process payment", description = "Process a pending payment (US-005-02-02)")
    public ResponseEntity<ApiResponse<PaymentDto>> processPayment(@PathVariable String paymentId) {
        PaymentDto payment = paymentService.processPayment(paymentId);
        return ResponseEntity.ok(ApiResponse.success(payment, "Payment processed successfully"));
    }

    @PostMapping("/{paymentId}/cancel")
    @Operation(summary = "Cancel payment", description = "Cancel a pending payment")
    public ResponseEntity<ApiResponse<PaymentDto>> cancelPayment(@PathVariable String paymentId) {
        PaymentDto payment = paymentService.cancelPayment(paymentId);
        return ResponseEntity.ok(ApiResponse.success(payment, "Payment cancelled successfully"));
    }
}
