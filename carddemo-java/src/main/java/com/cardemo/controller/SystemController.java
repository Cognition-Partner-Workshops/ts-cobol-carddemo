package com.cardemo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * System utilities controller.
 * Migrated from CDRD transaction / CODATE01 program.
 * COBOL: Returns current system date in various formats used by the application.
 */
@RestController
@RequestMapping("/system")
public class SystemController {

    /**
     * GET /system/date - Migrated from CDRD (CODATE01) date routine.
     * COBOL: ACCEPT WS-CURDATE FROM DATE YYYYMMDD
     *        ACCEPT WS-CURTIME FROM TIME
     * Returns date in multiple formats matching COBOL working storage fields.
     */
    @GetMapping("/date")
    public ResponseEntity<Map<String, String>> getSystemDate() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        Map<String, String> dateInfo = new LinkedHashMap<>();
        dateInfo.put("dateYyyyMmDd", today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        dateInfo.put("dateMmDdYyyy", today.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")));
        dateInfo.put("dateYyyyDdd", today.format(DateTimeFormatter.ofPattern("yyyy-DDD")));
        dateInfo.put("time", now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        dateInfo.put("timestamp", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")));

        return ResponseEntity.ok(dateInfo);
    }
}
