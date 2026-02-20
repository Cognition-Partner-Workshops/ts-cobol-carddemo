package com.carddemo.controller;

import com.carddemo.entity.Account;
import com.carddemo.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemInquiryController {

    private final AccountService accountService;

    public SystemInquiryController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/date")
    public ResponseEntity<Map<String, Object>> getSystemDate() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("systemDate", LocalDate.now().toString());
        response.put("systemTimestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts/{acctId}")
    public ResponseEntity<Account> inquireAccount(@PathVariable Long acctId) {
        return ResponseEntity.ok(accountService.getAccount(acctId));
    }
}
