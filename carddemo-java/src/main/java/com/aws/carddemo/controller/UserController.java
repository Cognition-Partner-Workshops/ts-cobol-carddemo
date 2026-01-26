package com.aws.carddemo.controller;

import com.aws.carddemo.dto.UserDto;
import com.aws.carddemo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUser(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/admins")
    public ResponseEntity<List<UserDto>> getAdminUsers() {
        return ResponseEntity.ok(userService.getAdminUsers());
    }

    @GetMapping("/regular")
    public ResponseEntity<List<UserDto>> getRegularUsers() {
        return ResponseEntity.ok(userService.getRegularUsers());
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserDto>> searchByLastName(@RequestParam String lastName) {
        return ResponseEntity.ok(userService.searchByLastName(lastName));
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(dto));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserDto> updateUser(@PathVariable String userId, @Valid @RequestBody UserDto dto) {
        return ResponseEntity.ok(userService.updateUser(userId, dto));
    }

    @PatchMapping("/{userId}/password")
    public ResponseEntity<Void> updatePassword(@PathVariable String userId, @RequestBody String newPassword) {
        userService.updatePassword(userId, newPassword);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/enable")
    public ResponseEntity<Void> enableUser(@PathVariable String userId) {
        userService.enableUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/disable")
    public ResponseEntity<Void> disableUser(@PathVariable String userId) {
        userService.disableUser(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count/admins")
    public ResponseEntity<Long> countAdminUsers() {
        return ResponseEntity.ok(userService.countAdminUsers());
    }

    @GetMapping("/count/regular")
    public ResponseEntity<Long> countRegularUsers() {
        return ResponseEntity.ok(userService.countRegularUsers());
    }
}
