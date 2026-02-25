package com.cardemo.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Menu controller.
 * Migrated from CM00 transaction / COMEN01C program.
 * COBOL: Displays main menu options based on user type (regular vs admin).
 */
@RestController
public class MenuController {

    /**
     * GET /menu - Migrated from CM00 (COMEN01C) main menu screen.
     * Returns available menu options based on user role.
     */
    @GetMapping("/menu")
    public ResponseEntity<Map<String, Object>> getMenu(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        Map<String, Object> menu = new LinkedHashMap<>();
        menu.put("userId", authentication.getName());
        menu.put("isAdmin", isAdmin);

        List<Map<String, String>> options = new ArrayList<>();

        // Common user options - COBOL: COMEN01C displays these for all users
        options.add(menuItem("CAVW", "Account View", "GET /accounts/{id}"));
        options.add(menuItem("CAUP", "Account Update", "PUT /accounts/{id}"));
        options.add(menuItem("CCLI", "Card List", "GET /cards?accountId=..."));
        options.add(menuItem("CCDL", "Card Detail", "GET /cards/{cardNum}"));
        options.add(menuItem("CCUP", "Card Update", "PUT /cards/{cardNum}"));
        options.add(menuItem("CT00", "Transaction List", "GET /transactions?cardNum=..."));
        options.add(menuItem("CT01", "Transaction Detail", "GET /transactions/{id}"));
        options.add(menuItem("CT02", "Transaction Add", "POST /transactions"));
        options.add(menuItem("CR00", "Transaction Report", "GET /reports/transactions"));
        options.add(menuItem("CB00", "Bill Payment", "POST /payments"));

        if (isAdmin) {
            // Admin-only options - COBOL: COMEN01C only shows these for admin users
            options.add(menuItem("CA00", "Admin Menu", "GET /admin/menu"));
            options.add(menuItem("CU00", "User List", "GET /admin/users"));
            options.add(menuItem("CU01", "User Add", "POST /admin/users"));
            options.add(menuItem("CU02", "User Update", "PUT /admin/users/{id}"));
            options.add(menuItem("CU03", "User Delete", "DELETE /admin/users/{id}"));
        }

        menu.put("options", options);
        return ResponseEntity.ok(menu);
    }

    private Map<String, String> menuItem(String txnId, String description, String endpoint) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("transactionId", txnId);
        item.put("description", description);
        item.put("endpoint", endpoint);
        return item;
    }
}
