package com.carddemo.customer.controller;

import com.carddemo.common.dto.ApiResponse;
import com.carddemo.common.dto.PageResponse;
import com.carddemo.customer.dto.CustomerDto;
import com.carddemo.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "Customer view operations")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/{customerId}")
    @Operation(summary = "Get customer by ID", description = "View customer details by Customer ID")
    public ResponseEntity<ApiResponse<CustomerDto>> getCustomer(@PathVariable String customerId) {
        CustomerDto customer = customerService.getCustomerById(customerId);
        return ResponseEntity.ok(ApiResponse.success(customer));
    }

    @GetMapping
    @Operation(summary = "List all customers", description = "Get paginated list of all customers")
    public ResponseEntity<ApiResponse<PageResponse<CustomerDto>>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "customerId") String sortBy) {
        PageResponse<CustomerDto> customers = customerService.getAllCustomers(page, size, sortBy);
        return ResponseEntity.ok(ApiResponse.success(customers));
    }

    @GetMapping("/search")
    @Operation(summary = "Search customers", description = "Search customers by last name")
    public ResponseEntity<ApiResponse<PageResponse<CustomerDto>>> searchCustomers(
            @RequestParam String lastName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<CustomerDto> customers = customerService.searchByLastName(lastName, page, size);
        return ResponseEntity.ok(ApiResponse.success(customers));
    }
}
