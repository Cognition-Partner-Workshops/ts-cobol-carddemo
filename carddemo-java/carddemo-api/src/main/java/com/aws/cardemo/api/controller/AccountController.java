package com.aws.cardemo.api.controller;

import com.aws.cardemo.domain.entity.Account;
import com.aws.cardemo.services.AccountService;
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
 * REST Controller for managing Account resources.
 * 
 * This controller provides endpoints for CRUD operations on credit card accounts,
 * as well as specialized queries for account management in the CardDemo application.
 * It serves as the primary interface for account-related operations in the modernized
 * mainframe application.
 * 
 * All endpoints are prefixed with /api/v1/accounts and return JSON responses.
 * The controller delegates business logic to the AccountService layer.
 * 
 * @author CardDemo Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Account", description = "Account management APIs for credit card account operations")
public class AccountController {

    private final AccountService accountService;

    /**
     * Retrieves all accounts from the system.
     * 
     * This endpoint returns a complete list of all credit card accounts stored in the database.
     * Use with caution in production environments as it may return a large dataset.
     * Consider implementing pagination for production use.
     * 
     * @return ResponseEntity containing a list of all Account entities with HTTP 200 status
     */
    @GetMapping
    @Operation(summary = "Get all accounts", 
               description = "Retrieves a complete list of all credit card accounts in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved all accounts")
    })
    public ResponseEntity<List<Account>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    /**
     * Retrieves a specific account by its unique identifier.
     * 
     * This endpoint fetches a single account based on the provided account ID.
     * Returns HTTP 404 if the account is not found.
     * 
     * @param accountId The unique identifier of the account to retrieve (11 characters max)
     * @return ResponseEntity containing the Account if found, or HTTP 404 if not found
     */
    @GetMapping("/{accountId}")
    @Operation(summary = "Get account by ID", 
               description = "Retrieves a specific account using its unique identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Account found and returned successfully"),
        @ApiResponse(responseCode = "404", description = "Account not found with the given ID")
    })
    public ResponseEntity<Account> getAccountById(
            @Parameter(description = "Unique account identifier", required = true)
            @PathVariable String accountId) {
        return accountService.getAccountById(accountId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new credit card account.
     * 
     * This endpoint accepts account details in the request body and creates a new account
     * in the system. The account data is validated before creation.
     * 
     * @param account The Account entity containing the details for the new account
     * @return ResponseEntity containing the created Account with HTTP 201 status
     */
    @PostMapping
    @Operation(summary = "Create a new account", 
               description = "Creates a new credit card account with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Account created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid account data provided")
    })
    public ResponseEntity<Account> createAccount(
            @Parameter(description = "Account details for creation", required = true)
            @Valid @RequestBody Account account) {
        Account created = accountService.createAccount(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Updates an existing credit card account.
     * 
     * This endpoint updates the account identified by the accountId path parameter
     * with the data provided in the request body. The account ID in the path takes
     * precedence over any ID in the request body.
     * 
     * @param accountId The unique identifier of the account to update
     * @param account The Account entity containing the updated details
     * @return ResponseEntity containing the updated Account with HTTP 200 status
     */
    @PutMapping("/{accountId}")
    @Operation(summary = "Update an existing account", 
               description = "Updates an existing account with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Account updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid account data provided"),
        @ApiResponse(responseCode = "404", description = "Account not found with the given ID")
    })
    public ResponseEntity<Account> updateAccount(
            @Parameter(description = "Unique account identifier", required = true)
            @PathVariable String accountId,
            @Parameter(description = "Updated account details", required = true)
            @Valid @RequestBody Account account) {
        account.setAccountId(accountId);
        Account updated = accountService.updateAccount(account);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a credit card account from the system.
     * 
     * This endpoint permanently removes the account identified by the accountId.
     * This operation cannot be undone. Consider implementing soft delete for production use.
     * 
     * @param accountId The unique identifier of the account to delete
     * @return ResponseEntity with HTTP 204 (No Content) status on successful deletion
     */
    @DeleteMapping("/{accountId}")
    @Operation(summary = "Delete an account", 
               description = "Permanently removes an account from the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Account deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Account not found with the given ID")
    })
    public ResponseEntity<Void> deleteAccount(
            @Parameter(description = "Unique account identifier", required = true)
            @PathVariable String accountId) {
        accountService.deleteAccount(accountId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves all accounts with a specific status.
     * 
     * This endpoint filters accounts based on their status code.
     * Valid status codes are: 'A' (Active), 'C' (Closed), 'S' (Suspended).
     * 
     * @param status The account status code to filter by (single character)
     * @return ResponseEntity containing a list of accounts matching the status
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "Get accounts by status", 
               description = "Retrieves all accounts matching the specified status code")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved accounts by status")
    })
    public ResponseEntity<List<Account>> getAccountsByStatus(
            @Parameter(description = "Account status code (A=Active, C=Closed, S=Suspended)", required = true)
            @PathVariable String status) {
        return ResponseEntity.ok(accountService.getAccountsByStatus(status));
    }

    /**
     * Retrieves all accounts belonging to a specific group.
     * 
     * This endpoint filters accounts based on their group identifier.
     * Groups are used to organize accounts for reporting and management purposes.
     * 
     * @param groupId The group identifier to filter accounts by
     * @return ResponseEntity containing a list of accounts in the specified group
     */
    @GetMapping("/group/{groupId}")
    @Operation(summary = "Get accounts by group ID", 
               description = "Retrieves all accounts belonging to a specific group")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved accounts by group")
    })
    public ResponseEntity<List<Account>> getAccountsByGroupId(
            @Parameter(description = "Group identifier for filtering accounts", required = true)
            @PathVariable String groupId) {
        return ResponseEntity.ok(accountService.getAccountsByGroupId(groupId));
    }

    /**
     * Retrieves all accounts that have exceeded their credit limit.
     * 
     * This endpoint returns accounts where the current balance exceeds the credit limit.
     * This is useful for risk management and collections processes.
     * 
     * @return ResponseEntity containing a list of accounts over their credit limit
     */
    @GetMapping("/over-limit")
    @Operation(summary = "Get accounts over credit limit", 
               description = "Retrieves all accounts where current balance exceeds credit limit")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved over-limit accounts")
    })
    public ResponseEntity<List<Account>> getAccountsOverCreditLimit() {
        return ResponseEntity.ok(accountService.getAccountsOverCreditLimit());
    }
}
