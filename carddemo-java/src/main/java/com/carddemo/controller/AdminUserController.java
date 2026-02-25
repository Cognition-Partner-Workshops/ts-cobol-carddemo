package com.carddemo.controller;

import com.carddemo.entity.User;
import com.carddemo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin User controller - migrated from:
 *   COUSR00C (CU00 - List Users)
 *   COUSR01C (CU01 - Add User)
 *   COUSR02C (CU02 - Update User)
 *   COUSR03C (CU03 - Delete User)
 *
 * All endpoints require ROLE_ADMIN (replaces COBOL SEC-USR-TYPE = 'A' check).
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/admin/users - List users with pagination (CU00).
     * Replaces COUSR00C STARTBR/READNEXT on USRSEC.
     */
    @GetMapping
    public ResponseEntity<Page<User>> listUsers(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(userService.listUsers(pageable));
    }

    /**
     * GET /api/admin/users/{userId} - Get user details.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@PathVariable String userId) {
        User user = userService.getUser(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.ok(user);
    }

    /**
     * POST /api/admin/users - Add new user (CU01).
     * Replaces COUSR01C WRITE to USRSEC.
     */
    @PostMapping
    public ResponseEntity<User> addUser(@Valid @RequestBody User user) {
        User created = userService.addUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/admin/users/{userId} - Update user (CU02).
     * Replaces COUSR02C READ/REWRITE on USRSEC.
     */
    @PutMapping("/{userId}")
    public ResponseEntity<User> updateUser(@PathVariable String userId, @RequestBody User updatedData) {
        User updated = userService.updateUser(userId, updatedData);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/admin/users/{userId} - Delete user (CU03).
     * Replaces COUSR03C READ/DELETE on USRSEC.
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
