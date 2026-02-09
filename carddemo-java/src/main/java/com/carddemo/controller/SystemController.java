package com.carddemo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    @GetMapping("/date")
    public ResponseEntity<Map<String, String>> getSystemDate() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("application", "CardDemo Java Migration");
        return ResponseEntity.ok(result);
    }
}
