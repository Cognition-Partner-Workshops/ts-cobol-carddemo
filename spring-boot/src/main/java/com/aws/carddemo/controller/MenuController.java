package com.aws.carddemo.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    public record MenuItem(String code, String label, String path) {}

    private static final List<MenuItem> COMMON_OPTIONS = List.of(
            new MenuItem("ACCT", "Account Management", "/api/accounts"),
            new MenuItem("CARD", "Credit Card Management", "/api/cards"),
            new MenuItem("TRAN", "Transaction Management", "/api/transactions"),
            new MenuItem("REPT", "Reports", "/api/reports"),
            new MenuItem("BILL", "Bill Payment", "/api/billing")
    );

    private static final MenuItem USER_MGMT_OPTION =
            new MenuItem("USER", "User Management", "/api/users");

    @GetMapping("/main")
    public ResponseEntity<Map<String, Object>> mainMenu(Authentication authentication) {
        List<MenuItem> options = new ArrayList<>(COMMON_OPTIONS);

        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        if (isAdmin) {
            options.add(USER_MGMT_OPTION);
        }

        return ResponseEntity.ok(Map.of(
                "userId", authentication.getName(),
                "options", options
        ));
    }

    @GetMapping("/admin")
    public ResponseEntity<Map<String, Object>> adminMenu(Authentication authentication) {
        List<MenuItem> options = new ArrayList<>(COMMON_OPTIONS);
        options.add(USER_MGMT_OPTION);

        return ResponseEntity.ok(Map.of(
                "userId", authentication.getName(),
                "options", options
        ));
    }
}
