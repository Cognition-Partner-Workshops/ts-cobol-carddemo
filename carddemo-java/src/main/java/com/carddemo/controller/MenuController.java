package com.carddemo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getMainMenu() {
        Map<String, Object> menu = new LinkedHashMap<>();
        menu.put("title", "CardDemo Main Menu");
        Map<String, String> options = new LinkedHashMap<>();
        options.put("01", "View Account");
        options.put("02", "Update Account");
        options.put("03", "View Credit Card");
        options.put("04", "Credit Card List");
        options.put("05", "View Transaction");
        options.put("06", "Transaction List");
        options.put("07", "Add Transaction");
        options.put("08", "Transaction Report");
        options.put("09", "Bill Payment");
        options.put("10", "Exit");
        options.put("11", "Authorization Summary");
        menu.put("options", options);
        return ResponseEntity.ok(menu);
    }
}
