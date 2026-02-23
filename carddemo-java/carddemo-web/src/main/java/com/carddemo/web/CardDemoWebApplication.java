package com.carddemo.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CardDemo Web Application entry point.
 * Thymeleaf-based web UI replacing BMS maps and 3270 screens.
 *
 * BMS → Thymeleaf mapping:
 *   COSGN0A.bms → login.html
 *   COMEN01.bms → menu.html
 *   COACTVW.bms → account-view.html
 *   COCRDLI.bms → card-list.html
 *   COTRN00.bms → transaction-list.html
 *
 * Phase 2: Placeholder application class.
 * Phase 3+: Implement Thymeleaf templates for each BMS map.
 */
@SpringBootApplication(scanBasePackages = "com.carddemo.web")
public class CardDemoWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardDemoWebApplication.class, args);
    }
}
