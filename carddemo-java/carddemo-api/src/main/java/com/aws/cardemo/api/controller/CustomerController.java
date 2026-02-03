package com.aws.cardemo.api.controller;

import com.aws.cardemo.domain.entity.Customer;
import com.aws.cardemo.services.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer", description = "Customer management APIs")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @Operation(summary = "Get all customers")
    public ResponseEntity<List<Customer>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<Customer> getCustomerById(@PathVariable String customerId) {
        return customerService.getCustomerById(customerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new customer")
    public ResponseEntity<Customer> createCustomer(@Valid @RequestBody Customer customer) {
        Customer created = customerService.createCustomer(customer);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{customerId}")
    @Operation(summary = "Update an existing customer")
    public ResponseEntity<Customer> updateCustomer(
            @PathVariable String customerId,
            @Valid @RequestBody Customer customer) {
        customer.setCustomerId(customerId);
        Customer updated = customerService.updateCustomer(customer);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{customerId}")
    @Operation(summary = "Delete a customer")
    public ResponseEntity<Void> deleteCustomer(@PathVariable String customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Search customers by name")
    public ResponseEntity<List<Customer>> searchCustomersByName(@RequestParam String name) {
        return ResponseEntity.ok(customerService.searchCustomersByName(name));
    }

    @GetMapping("/state/{stateCode}")
    @Operation(summary = "Get customers by state")
    public ResponseEntity<List<Customer>> getCustomersByState(@PathVariable String stateCode) {
        return ResponseEntity.ok(customerService.getCustomersByState(stateCode));
    }

    @GetMapping("/credit-score")
    @Operation(summary = "Get customers by minimum credit score")
    public ResponseEntity<List<Customer>> getCustomersByMinimumCreditScore(@RequestParam Integer minScore) {
        return ResponseEntity.ok(customerService.getCustomersByMinimumCreditScore(minScore));
    }
}
