package com.carddemo.controller;

import com.carddemo.dto.UserDto;
import com.carddemo.entity.TransactionCategory;
import com.carddemo.entity.TransactionType;
import com.carddemo.entity.User;
import com.carddemo.service.TransactionTypeService;
import com.carddemo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final TransactionTypeService transactionTypeService;

    public AdminController(UserService userService, TransactionTypeService transactionTypeService) {
        this.userService = userService;
        this.transactionTypeService = transactionTypeService;
    }

    @GetMapping("/menu")
    public ResponseEntity<Map<String, Object>> getAdminMenu() {
        Map<String, Object> menu = new LinkedHashMap<>();
        menu.put("title", "CardDemo Admin Menu");
        Map<String, String> options = new LinkedHashMap<>();
        options.put("01", "User List");
        options.put("02", "User Add");
        options.put("03", "User Update");
        options.put("04", "User Delete");
        options.put("05", "Transaction Type List");
        options.put("06", "Transaction Type Add/Update");
        menu.put("options", options);
        return ResponseEntity.ok(menu);
    }

    @GetMapping("/users")
    public ResponseEntity<Page<User>> listUsers(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(userService.listUsers(pageable));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUser(@PathVariable("id") String userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    @PostMapping("/users")
    public ResponseEntity<User> addUser(@Valid @RequestBody UserDto dto) {
        User user = userService.addUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUser(@PathVariable("id") String userId,
                                           @Valid @RequestBody UserDto dto) {
        User user = userService.updateUser(userId, dto);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") String userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/transaction-types")
    public ResponseEntity<List<TransactionType>> listTransactionTypes() {
        return ResponseEntity.ok(transactionTypeService.listTransactionTypes());
    }

    @GetMapping("/transaction-types/{typeCd}")
    public ResponseEntity<TransactionType> getTransactionType(@PathVariable String typeCd) {
        return ResponseEntity.ok(transactionTypeService.getTransactionType(typeCd));
    }

    @PostMapping("/transaction-types")
    public ResponseEntity<TransactionType> addTransactionType(@RequestBody TransactionType type) {
        TransactionType created = transactionTypeService.addTransactionType(type);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/transaction-types/{typeCd}")
    public ResponseEntity<TransactionType> updateTransactionType(
            @PathVariable String typeCd, @RequestBody TransactionType type) {
        return ResponseEntity.ok(transactionTypeService.updateTransactionType(typeCd, type));
    }

    @DeleteMapping("/transaction-types/{typeCd}")
    public ResponseEntity<Void> deleteTransactionType(@PathVariable String typeCd) {
        transactionTypeService.deleteTransactionType(typeCd);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/transaction-types/{typeCd}/categories")
    public ResponseEntity<List<TransactionCategory>> listCategories(@PathVariable String typeCd) {
        return ResponseEntity.ok(transactionTypeService.listCategories(typeCd));
    }

    @PostMapping("/transaction-types/{typeCd}/categories")
    public ResponseEntity<TransactionCategory> addCategory(
            @PathVariable String typeCd, @RequestBody TransactionCategory category) {
        category.setTypeCd(typeCd);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionTypeService.addCategory(category));
    }

    @DeleteMapping("/transaction-types/{typeCd}/categories/{catCd}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable String typeCd, @PathVariable Integer catCd) {
        transactionTypeService.deleteCategory(typeCd, catCd);
        return ResponseEntity.noContent().build();
    }
}
