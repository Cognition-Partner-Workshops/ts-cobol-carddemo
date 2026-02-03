package com.aws.cardemo.api.controller;

import com.aws.cardemo.domain.entity.Customer;
import com.aws.cardemo.services.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST Controller for managing Customer resources.
 * 
 * This controller provides endpoints for CRUD operations on credit card customers,
 * as well as specialized search and filtering capabilities. It handles all customer-related
 * operations in the CardDemo application, including customer profile management,
 * search functionality, and credit score filtering.
 * 
 * All endpoints are prefixed with /api/v1/customers and return JSON responses.
 * The controller delegates business logic to the CustomerService layer.
 * 
 * @author CardDemo Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer", description = "Customer management APIs for credit card customer operations")
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Retrieves all customers from the system.
     * 
     * This endpoint returns a complete list of all customers stored in the database.
     * Use with caution in production environments as it may return a large dataset.
     * Consider implementing pagination for production use.
     * 
     * @return ResponseEntity containing a list of all Customer entities with HTTP 200 status
     */
    @GetMapping
    @Operation(summary = "Get all customers", 
               description = "Retrieves a complete list of all customers in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved all customers")
    })
    public ResponseEntity<List<Customer>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    /**
     * Retrieves a specific customer by their unique identifier.
     * 
     * This endpoint fetches a single customer based on the provided customer ID.
     * Returns HTTP 404 if the customer is not found.
     * 
     * @param customerId The unique identifier of the customer to retrieve (9 characters max)
     * @return ResponseEntity containing the Customer if found, or HTTP 404 if not found
     */
    @GetMapping("/{customerId}")
    @Operation(summary = "Get customer by ID", 
               description = "Retrieves a specific customer using their unique identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Customer found and returned successfully"),
        @ApiResponse(responseCode = "404", description = "Customer not found with the given ID")
    })
    public ResponseEntity<Customer> getCustomerById(
            @Parameter(description = "Unique customer identifier", required = true)
            @PathVariable String customerId) {
        return customerService.getCustomerById(customerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new customer record.
     * 
     * This endpoint accepts customer details in the request body and creates a new customer
     * in the system. The customer data is validated before creation, including required
     * fields like first name and last name.
     * 
     * @param customer The Customer entity containing the details for the new customer
     * @return ResponseEntity containing the created Customer with HTTP 201 status
     */
    @PostMapping
    @Operation(summary = "Create a new customer", 
               description = "Creates a new customer record with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Customer created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid customer data provided")
    })
    public ResponseEntity<Customer> createCustomer(
            @Parameter(description = "Customer details for creation", required = true)
            @Valid @RequestBody Customer customer) {
        Customer created = customerService.createCustomer(customer);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Updates an existing customer record.
     * 
     * This endpoint updates the customer identified by the customerId path parameter
     * with the data provided in the request body. The customer ID in the path takes
     * precedence over any ID in the request body.
     * 
     * @param customerId The unique identifier of the customer to update
     * @param customer The Customer entity containing the updated details
     * @return ResponseEntity containing the updated Customer with HTTP 200 status
     */
    @PutMapping("/{customerId}")
    @Operation(summary = "Update an existing customer", 
               description = "Updates an existing customer record with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Customer updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid customer data provided"),
        @ApiResponse(responseCode = "404", description = "Customer not found with the given ID")
    })
    public ResponseEntity<Customer> updateCustomer(
            @Parameter(description = "Unique customer identifier", required = true)
            @PathVariable String customerId,
            @Parameter(description = "Updated customer details", required = true)
            @Valid @RequestBody Customer customer) {
        customer.setCustomerId(customerId);
        Customer updated = customerService.updateCustomer(customer);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a customer record from the system.
     * 
     * This endpoint permanently removes the customer identified by the customerId.
     * This operation cannot be undone. Consider implementing soft delete for production use.
     * Note: Associated accounts and cards should be handled appropriately before deletion.
     * 
     * @param customerId The unique identifier of the customer to delete
     * @return ResponseEntity with HTTP 204 (No Content) status on successful deletion
     */
    @DeleteMapping("/{customerId}")
    @Operation(summary = "Delete a customer", 
               description = "Permanently removes a customer record from the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Customer deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Customer not found with the given ID")
    })
    public ResponseEntity<Void> deleteCustomer(
            @Parameter(description = "Unique customer identifier", required = true)
            @PathVariable String customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Searches for customers by name.
     * 
     * This endpoint performs a partial match search on both first name and last name fields.
     * The search is case-insensitive and returns all customers whose first or last name
     * contains the search term.
     * 
     * @param name The search term to match against customer names
     * @return ResponseEntity containing a list of customers matching the search criteria
     */
    @GetMapping("/search")
    @Operation(summary = "Search customers by name", 
               description = "Searches for customers whose first or last name contains the search term")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved matching customers")
    })
    public ResponseEntity<List<Customer>> searchCustomersByName(
            @Parameter(description = "Search term to match against customer names", required = true)
            @RequestParam String name) {
        return ResponseEntity.ok(customerService.searchCustomersByName(name));
    }

    /**
     * Retrieves all customers in a specific state.
     * 
     * This endpoint filters customers based on their state code (e.g., 'CA', 'NY', 'TX').
     * Useful for regional reporting and marketing campaigns.
     * 
     * @param stateCode The two-letter state code to filter customers by
     * @return ResponseEntity containing a list of customers in the specified state
     */
    @GetMapping("/state/{stateCode}")
    @Operation(summary = "Get customers by state", 
               description = "Retrieves all customers residing in the specified state")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved customers by state")
    })
    public ResponseEntity<List<Customer>> getCustomersByState(
            @Parameter(description = "Two-letter state code (e.g., CA, NY, TX)", required = true)
            @PathVariable String stateCode) {
        return ResponseEntity.ok(customerService.getCustomersByState(stateCode));
    }

    /**
     * Retrieves customers with a minimum FICO credit score.
     * 
     * This endpoint filters customers based on their FICO credit score, returning
     * only those with a score equal to or greater than the specified minimum.
     * Useful for credit limit reviews and promotional offers.
     * 
     * @param minScore The minimum FICO credit score threshold (typically 300-850)
     * @return ResponseEntity containing a list of customers meeting the credit score criteria
     */
    @GetMapping("/credit-score")
    @Operation(summary = "Get customers by minimum credit score", 
               description = "Retrieves customers with FICO credit score at or above the specified minimum")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved customers by credit score")
    })
    public ResponseEntity<List<Customer>> getCustomersByMinimumCreditScore(
            @Parameter(description = "Minimum FICO credit score threshold (300-850)", required = true)
            @RequestParam Integer minScore) {
        return ResponseEntity.ok(customerService.getCustomersByMinimumCreditScore(minScore));
    }
}
