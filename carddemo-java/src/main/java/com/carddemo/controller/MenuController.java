package com.carddemo.controller;

import com.carddemo.dto.MenuOption;
import com.carddemo.service.MenuService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Menu controller - migrated from COMEN01C (CM00 Main Menu) and COADM01C (Admin Menu).
 * Replaces CICS transaction CM00/CA00 with REST endpoint.
 */
@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    /**
     * GET /api/menu - Get menu options based on user role.
     * Admin users get additional menu options (user mgmt, transaction type mgmt).
     */
    @GetMapping
    public ResponseEntity<List<MenuOption>> getMenu(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        List<MenuOption> options = isAdmin
                ? menuService.getAdminMenuOptions()
                : menuService.getUserMenuOptions();

        return ResponseEntity.ok(options);
    }
}
