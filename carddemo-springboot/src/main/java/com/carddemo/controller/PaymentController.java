package com.carddemo.controller;

import com.carddemo.service.PaymentService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for bill payment processing.
 * Replaces CICS bill payment transactions:
 * - COBIL00C.cbl (Bill Payment)
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
