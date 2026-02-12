package com.carddemo.controller;

import com.carddemo.service.CustomerService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for customer management.
 * Replaces CICS customer management transactions:
 * - COCUSTIC.cbl (Customer Inquiry)
 * - COCUSTPC.cbl (Customer Update)
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }
}
