package com.carddemo.api.controller;

import com.carddemo.api.dto.AccountResponse;
import com.carddemo.api.dto.AccountUpdateRequest;
import com.carddemo.api.dto.PageResponse;
import com.carddemo.api.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Account REST controller.
 * Replaces CICS transactions CAVW (COACTVWC) and CAUP (COACTUPC).
 *
 * COBOL → Java mapping:
 *   CAVW → GET  /api/accounts/{accountId}  (Account View)
 *   CAUP → PUT  /api/accounts/{accountId}  (Account Update)
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account management (replaces CICS CAVW/CAUP)")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/{accountId}")
    @Operation(summary = "View account details", description = "Retrieves account by ID (replaces CAVW)")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.getAccount(accountId));
    }

    @PutMapping("/{accountId}")
    @Operation(summary = "Update account", description = "Updates account fields (replaces CAUP)")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable Long accountId,
            @Valid @RequestBody AccountUpdateRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(accountId, request));
    }

    @GetMapping
    @Operation(summary = "List accounts", description = "Paginated account listing")
    public ResponseEntity<PageResponse<AccountResponse>> listAccounts(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(accountService.listAccounts(
                PageRequest.of(page, size, Sort.by("acctId"))));
    }
}
