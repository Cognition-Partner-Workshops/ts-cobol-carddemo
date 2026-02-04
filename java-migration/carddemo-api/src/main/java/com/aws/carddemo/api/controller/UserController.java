package com.aws.carddemo.api.controller;

import com.aws.carddemo.service.dto.UserDTO;
import com.aws.carddemo.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints - migrated from COUSR00C, COUSR01C, COUSR02C, COUSR03C")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List all users with pagination")
    public ResponseEntity<Page<UserDTO>> listUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.listUsers(pageable));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserDTO> getUser(@PathVariable String userId) {
        return userService.getUser(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create new user")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserService.UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update user")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UserService.UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUser(userId, request));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admins")
    @Operation(summary = "List admin users")
    public ResponseEntity<List<UserDTO>> listAdmins() {
        return ResponseEntity.ok(userService.listAdmins());
    }

    @GetMapping("/search")
    @Operation(summary = "Search users by name")
    public ResponseEntity<Page<UserDTO>> searchByName(@RequestParam String name, Pageable pageable) {
        return ResponseEntity.ok(userService.searchByName(name, pageable));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get user statistics")
    public ResponseEntity<UserService.UserStatistics> getStatistics() {
        return ResponseEntity.ok(userService.getStatistics());
    }
}
