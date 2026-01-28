package com.carddemo.customer.controller;

import com.carddemo.common.dto.ApiResponse;
import com.carddemo.common.dto.CustomerDto;
import com.carddemo.common.dto.PagedResponse;
import com.carddemo.customer.dto.CreateCustomerRequest;
import com.carddemo.customer.dto.UpdateCustomerRequest;
import com.carddemo.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "Customer management endpoints")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    @Operation(summary = "Get all customers", description = "Get paginated list of all customers")
    public ResponseEntity<ApiResponse<PagedResponse<CustomerDto>>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "customerId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        PagedResponse<CustomerDto> customers = customerService.getAllCustomers(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(customers));
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "Get customer by ID", description = "Get customer details by customer ID")
    public ResponseEntity<ApiResponse<CustomerDto>> getCustomerById(@PathVariable Long customerId) {
        CustomerDto customer = customerService.getCustomerById(customerId);
        return ResponseEntity.ok(ApiResponse.success(customer));
    }

    @GetMapping("/search")
    @Operation(summary = "Search customers", description = "Search customers by name or SSN")
    public ResponseEntity<ApiResponse<PagedResponse<CustomerDto>>> searchCustomers(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponse<CustomerDto> customers = customerService.searchCustomers(q, page, size);
        return ResponseEntity.ok(ApiResponse.success(customers));
    }

    @PostMapping
    @Operation(summary = "Create customer", description = "Create a new customer")
    public ResponseEntity<ApiResponse<CustomerDto>> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerDto customer = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created successfully", customer));
    }

    @PutMapping("/{customerId}")
    @Operation(summary = "Update customer", description = "Update an existing customer")
    public ResponseEntity<ApiResponse<CustomerDto>> updateCustomer(
            @PathVariable Long customerId,
            @Valid @RequestBody UpdateCustomerRequest request) {
        CustomerDto customer = customerService.updateCustomer(customerId, request);
        return ResponseEntity.ok(ApiResponse.success("Customer updated successfully", customer));
    }

    @DeleteMapping("/{customerId}")
    @Operation(summary = "Delete customer", description = "Delete a customer")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable Long customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted successfully", null));
    }

    @GetMapping("/by-state/{stateCode}")
    @Operation(summary = "Get customers by state", description = "Get all customers in a specific state")
    public ResponseEntity<ApiResponse<List<CustomerDto>>> getCustomersByState(@PathVariable String stateCode) {
        List<CustomerDto> customers = customerService.getCustomersByState(stateCode);
        return ResponseEntity.ok(ApiResponse.success(customers));
    }

    @GetMapping("/by-fico-range")
    @Operation(summary = "Get customers by FICO range", description = "Get customers within a FICO score range")
    public ResponseEntity<ApiResponse<List<CustomerDto>>> getCustomersByFicoRange(
            @RequestParam Integer minScore,
            @RequestParam Integer maxScore) {
        List<CustomerDto> customers = customerService.getCustomersByFicoRange(minScore, maxScore);
        return ResponseEntity.ok(ApiResponse.success(customers));
    }
}
