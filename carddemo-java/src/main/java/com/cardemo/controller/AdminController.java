package com.cardemo.controller;

import com.cardemo.dto.UserRequest;
import com.cardemo.entity.CardDemoUser;
import com.cardemo.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin controller (requires ROLE_ADMIN).
 * Migrated from CA00 (COADM01C - admin menu),
 * CU00 (COUSR00C - list), CU01 (COUSR01C - add),
 * CU02 (COUSR02C - update), CU03 (COUSR03C - delete).
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UserManagementService userManagementService;

    public AdminController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    /**
     * GET /admin/menu - Migrated from CA00 (COADM01C) admin menu screen.
     */
    @GetMapping("/menu")
    public ResponseEntity<Map<String, Object>> getAdminMenu() {
        Map<String, Object> menu = new LinkedHashMap<>();
        menu.put("title", "Admin Menu");
        List<Map<String, String>> options = List.of(
            Map.of("txnId", "CU00", "desc", "User List", "endpoint", "GET /admin/users"),
            Map.of("txnId", "CU01", "desc", "User Add", "endpoint", "POST /admin/users"),
            Map.of("txnId", "CU02", "desc", "User Update", "endpoint", "PUT /admin/users/{id}"),
            Map.of("txnId", "CU03", "desc", "User Delete", "endpoint", "DELETE /admin/users/{id}")
        );
        menu.put("options", options);
        return ResponseEntity.ok(menu);
    }

    /**
     * GET /admin/users - Migrated from CU00 (COUSR00C) user list screen.
     */
    @GetMapping("/users")
    public ResponseEntity<Page<CardDemoUser>> listUsers(Pageable pageable) {
        return ResponseEntity.ok(userManagementService.listUsers(pageable));
    }

    /**
     * POST /admin/users - Migrated from CU01 (COUSR01C) user add screen.
     */
    @PostMapping("/users")
    public ResponseEntity<CardDemoUser> createUser(@Valid @RequestBody UserRequest request) {
        CardDemoUser created = userManagementService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /admin/users/{id} - Migrated from CU02 (COUSR02C) user update screen.
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<CardDemoUser> updateUser(@PathVariable("id") String id,
                                                   @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(userManagementService.updateUser(id, request));
    }

    /**
     * DELETE /admin/users/{id} - Migrated from CU03 (COUSR03C) user delete screen.
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") String id) {
        userManagementService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
