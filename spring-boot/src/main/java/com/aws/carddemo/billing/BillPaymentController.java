package com.aws.carddemo.billing;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aws.carddemo.billing.dto.BalanceResponse;
import com.aws.carddemo.billing.dto.BillPaymentRequest;
import com.aws.carddemo.billing.dto.BillPaymentResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/billing")
public class BillPaymentController {

    private final BillPaymentService billPaymentService;

    public BillPaymentController(BillPaymentService billPaymentService) {
        this.billPaymentService = billPaymentService;
    }

    @GetMapping("/balance")
    public ResponseEntity<BalanceResponse> getBalance(@RequestParam Long accountId) {
        return ResponseEntity.ok(billPaymentService.getBalance(accountId));
    }

    @PostMapping("/pay")
    public ResponseEntity<BillPaymentResponse> processPayment(
            @Valid @RequestBody BillPaymentRequest request) {
        return ResponseEntity.ok(billPaymentService.processPayment(request));
    }
}
