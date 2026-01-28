package com.carddemo.payment.controller;

import com.carddemo.common.dto.ApiResponse;
import com.carddemo.common.dto.PagedResponse;
import com.carddemo.payment.dto.CreatePaymentRequest;
import com.carddemo.payment.dto.PaymentDto;
import com.carddemo.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Bill payment and payment processing endpoints")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    @Operation(summary = "Get all payments", description = "Get paginated list of all payments")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentDto>>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PagedResponse<PaymentDto> payments = paymentService.getAllPayments(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID", description = "Get payment details by payment ID")
    public ResponseEntity<ApiResponse<PaymentDto>> getPaymentById(@PathVariable String paymentId) {
        PaymentDto payment = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get payments by account", description = "Get all payments for an account")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentDto>>> getPaymentsByAccountId(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponse<PaymentDto> payments = paymentService.getPaymentsByAccountId(accountId, page, size);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @PostMapping
    @Operation(summary = "Create payment", description = "Create a new payment")
    public ResponseEntity<ApiResponse<PaymentDto>> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        PaymentDto payment = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment created successfully", payment));
    }

    @PostMapping("/{paymentId}/process")
    @Operation(summary = "Process payment", description = "Process a pending or scheduled payment")
    public ResponseEntity<ApiResponse<PaymentDto>> processPayment(@PathVariable String paymentId) {
        PaymentDto payment = paymentService.processPaymentById(paymentId);
        return ResponseEntity.ok(ApiResponse.success("Payment processed successfully", payment));
    }

    @PostMapping("/{paymentId}/cancel")
    @Operation(summary = "Cancel payment", description = "Cancel a pending or scheduled payment")
    public ResponseEntity<ApiResponse<PaymentDto>> cancelPayment(@PathVariable String paymentId) {
        PaymentDto payment = paymentService.cancelPayment(paymentId);
        return ResponseEntity.ok(ApiResponse.success("Payment cancelled successfully", payment));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get payments by status", description = "Get all payments with a specific status")
    public ResponseEntity<ApiResponse<List<PaymentDto>>> getPaymentsByStatus(@PathVariable String status) {
        List<PaymentDto> payments = paymentService.getPaymentsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @GetMapping("/account/{accountId}/date-range")
    @Operation(summary = "Get payments by account and date range", description = "Get payments for an account within a date range")
    public ResponseEntity<ApiResponse<List<PaymentDto>>> getPaymentsByAccountAndDateRange(
            @PathVariable Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<PaymentDto> payments = paymentService.getPaymentsByAccountAndDateRange(accountId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @GetMapping("/account/{accountId}/total")
    @Operation(summary = "Get total payments by account", description = "Get total completed payments for an account")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalPaymentsByAccount(@PathVariable Long accountId) {
        BigDecimal total = paymentService.getTotalPaymentsByAccount(accountId);
        return ResponseEntity.ok(ApiResponse.success(total));
    }

    @PostMapping("/process-scheduled")
    @Operation(summary = "Process scheduled payments", description = "Process all scheduled payments that are due")
    public ResponseEntity<ApiResponse<Integer>> processScheduledPayments() {
        int processed = paymentService.processScheduledPayments();
        return ResponseEntity.ok(ApiResponse.success("Processed " + processed + " scheduled payments", processed));
    }
}
