package com.aws.carddemo.api.controller;

import com.aws.carddemo.service.account.BillPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bill-payments")
@RequiredArgsConstructor
@Tag(name = "Bill Payments", description = "Bill payment endpoints - migrated from COBIL00C")
public class BillPaymentController {

    private final BillPaymentService billPaymentService;

    @PostMapping
    @Operation(summary = "Process bill payment")
    public ResponseEntity<BillPaymentService.BillPaymentResult> processPayment(
            @Valid @RequestBody BillPaymentService.BillPaymentRequest request) {
        return ResponseEntity.ok(billPaymentService.processPayment(request));
    }
}
