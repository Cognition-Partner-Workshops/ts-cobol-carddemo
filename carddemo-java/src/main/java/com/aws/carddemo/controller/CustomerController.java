package com.aws.carddemo.controller;

import com.aws.carddemo.dto.CustomerDto;
import com.aws.carddemo.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/{custId}")
    public ResponseEntity<CustomerDto> getCustomer(@PathVariable Long custId) {
        return ResponseEntity.ok(customerService.getCustomer(custId));
    }

    @GetMapping("/{custId}/with-cards")
    public ResponseEntity<CustomerDto> getCustomerWithCardXrefs(@PathVariable Long custId) {
        return ResponseEntity.ok(customerService.getCustomerWithCardXrefs(custId));
    }

    @GetMapping("/ssn/{ssn}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerDto> getCustomerBySsn(@PathVariable String ssn) {
        return ResponseEntity.ok(customerService.getCustomerBySsn(ssn));
    }

    @GetMapping
    public ResponseEntity<Page<CustomerDto>> getAllCustomers(Pageable pageable) {
        return ResponseEntity.ok(customerService.getAllCustomers(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CustomerDto>> searchCustomers(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName) {
        if (firstName != null && lastName != null) {
            return ResponseEntity.ok(customerService.searchByName(firstName, lastName));
        } else if (lastName != null) {
            return ResponseEntity.ok(customerService.searchByLastName(lastName));
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/state/{stateCd}")
    public ResponseEntity<Page<CustomerDto>> getCustomersByState(@PathVariable String stateCd, Pageable pageable) {
        return ResponseEntity.ok(customerService.getCustomersByState(stateCd, pageable));
    }

    @GetMapping("/zip/{zip}")
    public ResponseEntity<List<CustomerDto>> getCustomersByZip(@PathVariable String zip) {
        return ResponseEntity.ok(customerService.getCustomersByZip(zip));
    }

    @GetMapping("/fico/min/{minScore}")
    public ResponseEntity<List<CustomerDto>> getCustomersByMinFicoScore(@PathVariable Integer minScore) {
        return ResponseEntity.ok(customerService.getCustomersByMinFicoScore(minScore));
    }

    @GetMapping("/primary-holders")
    public ResponseEntity<List<CustomerDto>> getPrimaryCardHolders() {
        return ResponseEntity.ok(customerService.getPrimaryCardHolders());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerDto> createCustomer(@Valid @RequestBody CustomerDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(dto));
    }

    @PutMapping("/{custId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerDto> updateCustomer(@PathVariable Long custId, @Valid @RequestBody CustomerDto dto) {
        return ResponseEntity.ok(customerService.updateCustomer(custId, dto));
    }

    @DeleteMapping("/{custId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long custId) {
        customerService.deleteCustomer(custId);
        return ResponseEntity.noContent().build();
    }
}
