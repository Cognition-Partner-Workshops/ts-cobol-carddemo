package com.carddemo.api.controller;

import com.carddemo.api.dto.PageResponse;
import com.carddemo.api.dto.UserCreateRequest;
import com.carddemo.api.dto.UserResponse;
import com.carddemo.api.dto.UserUpdateRequest;
import com.carddemo.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * User administration REST controller.
 * Replaces CICS transactions CU00-CU03 (COUSR00C-COUSR03C).
 *
 * COBOL → Java mapping:
 *   CU00 → GET    /api/admin/users            (List Users)
 *   CU01 → POST   /api/admin/users            (Add User)
 *   CU02 → PUT    /api/admin/users/{userId}   (Update User)
 *   CU03 → DELETE /api/admin/users/{userId}   (Delete User)
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "User Administration", description = "User management (replaces CICS CU00-CU03)")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List users", description = "Paginated user listing (replaces CU00)")
    public ResponseEntity<PageResponse<UserResponse>> listUsers(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userService.listUsers(
                PageRequest.of(page, size, Sort.by("usrId"))));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user", description = "Retrieves user by ID")
    public ResponseEntity<UserResponse> getUser(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    @PostMapping
    @Operation(summary = "Create user", description = "Creates a new user (replaces CU01)")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update user", description = "Updates user fields (replaces CU02)")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUser(userId, request));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user", description = "Deletes a user (replaces CU03)")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
