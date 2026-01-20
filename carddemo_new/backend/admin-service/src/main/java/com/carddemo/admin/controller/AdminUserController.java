package com.carddemo.admin.controller;

import com.carddemo.admin.dto.AdminUserDto;
import com.carddemo.admin.dto.CreateUserRequest;
import com.carddemo.admin.dto.UpdateUserRequest;
import com.carddemo.admin.service.AdminUserService;
import com.carddemo.common.dto.ApiResponse;
import com.carddemo.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "User Administration", description = "User management operations - EPIC-007")
public class AdminUserController {

    private final AdminUserService userService;

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "View user details by User ID")
    public ResponseEntity<ApiResponse<AdminUserDto>> getUser(@PathVariable String userId) {
        AdminUserDto user = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @GetMapping
    @Operation(summary = "List all users", description = "Get paginated list of all users (US-007-01-01)")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "userId") String sortBy) {
        PageResponse<AdminUserDto> users = userService.getAllUsers(page, size, sortBy);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/type/{userType}")
    @Operation(summary = "List users by type", description = "Get users filtered by type (A=Admin, U=User) (US-007-01-02)")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserDto>>> getUsersByType(
            @PathVariable String userType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<AdminUserDto> users = userService.getUsersByType(userType, page, size);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/search")
    @Operation(summary = "Search users", description = "Search users by last name (US-007-01-03)")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserDto>>> searchUsers(
            @RequestParam String lastName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<AdminUserDto> users = userService.searchUsersByLastName(lastName, page, size);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PostMapping
    @Operation(summary = "Create user", description = "Create a new user account (US-007-02-01 to US-007-02-03)")
    public ResponseEntity<ApiResponse<AdminUserDto>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        AdminUserDto user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user, "User created successfully"));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update user", description = "Update user information (US-007-03-01 to US-007-03-03)")
    public ResponseEntity<ApiResponse<AdminUserDto>> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRequest request) {
        AdminUserDto user = userService.updateUser(userId, request);
        return ResponseEntity.ok(ApiResponse.success(user, "User updated successfully"));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user (soft)", description = "Deactivate a user account (US-007-04-01 to US-007-04-03)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User deactivated successfully"));
    }

    @DeleteMapping("/{userId}/permanent")
    @Operation(summary = "Delete user (hard)", description = "Permanently delete a user account")
    public ResponseEntity<ApiResponse<Void>> hardDeleteUser(@PathVariable String userId) {
        userService.hardDeleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted permanently"));
    }
}
