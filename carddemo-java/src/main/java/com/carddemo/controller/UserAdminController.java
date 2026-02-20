package com.carddemo.controller;

import com.carddemo.dto.UserCreateRequest;
import com.carddemo.entity.UserSecurity;
import com.carddemo.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public ResponseEntity<Page<UserSecurity>> listUsers(Pageable pageable) {
        return ResponseEntity.ok(userAdminService.listUsers(pageable));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserSecurity> getUser(@PathVariable String userId) {
        return ResponseEntity.ok(userAdminService.getUser(userId));
    }

    @PostMapping
    public ResponseEntity<UserSecurity> createUser(
            @Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.ok(userAdminService.createUser(request));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserSecurity> updateUser(@PathVariable String userId,
                                                   @Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.ok(userAdminService.updateUser(userId, request));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        userAdminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
