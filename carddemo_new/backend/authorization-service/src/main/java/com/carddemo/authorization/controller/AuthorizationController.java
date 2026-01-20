package com.carddemo.authorization.controller;

import com.carddemo.authorization.dto.AuthorizationRequestDto;
import com.carddemo.authorization.dto.AuthorizationResponseDto;
import com.carddemo.authorization.dto.AuthorizationRuleDto;
import com.carddemo.authorization.entity.AuthorizationRequest;
import com.carddemo.authorization.entity.AuthorizationRule;
import com.carddemo.authorization.service.AuthorizationService;
import com.carddemo.authorization.service.AuthorizationRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/authorization")
@RequiredArgsConstructor
@Slf4j
public class AuthorizationController {
    
    private final AuthorizationService authorizationService;
    private final AuthorizationRuleService ruleService;
    
    // EPIC-009 Feature 1: Real-time Authorization Processing
    @PostMapping("/process")
    public ResponseEntity<AuthorizationResponseDto> processAuthorization(
            @RequestBody AuthorizationRequestDto request) {
        log.info("Received authorization request");
        AuthorizationResponseDto response = authorizationService.processAuthorization(request);
        return ResponseEntity.ok(response);
    }
    
    // EPIC-009 Feature 2: Authorization History
    @GetMapping("/history/card/{cardNumber}")
    public ResponseEntity<List<AuthorizationRequest>> getCardHistory(@PathVariable String cardNumber) {
        List<AuthorizationRequest> history = authorizationService.getAuthorizationHistory(cardNumber);
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/history/account/{accountId}")
    public ResponseEntity<List<AuthorizationRequest>> getAccountHistory(@PathVariable String accountId) {
        List<AuthorizationRequest> history = authorizationService.getAuthorizationsByAccount(accountId);
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/{authId}")
    public ResponseEntity<AuthorizationRequest> getAuthorization(@PathVariable String authId) {
        return authorizationService.getAuthorization(authId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // EPIC-009 Feature 3: Authorization Rules Management
    @GetMapping("/rules")
    public ResponseEntity<List<AuthorizationRule>> getAllRules() {
        List<AuthorizationRule> rules = ruleService.getAllRules();
        return ResponseEntity.ok(rules);
    }
    
    @GetMapping("/rules/active")
    public ResponseEntity<List<AuthorizationRule>> getActiveRules() {
        List<AuthorizationRule> rules = ruleService.getActiveRules();
        return ResponseEntity.ok(rules);
    }
    
    @GetMapping("/rules/{ruleCode}")
    public ResponseEntity<AuthorizationRule> getRule(@PathVariable String ruleCode) {
        return ruleService.getRuleByCode(ruleCode)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/rules/type/{ruleType}")
    public ResponseEntity<List<AuthorizationRule>> getRulesByType(@PathVariable String ruleType) {
        List<AuthorizationRule> rules = ruleService.getRulesByType(ruleType);
        return ResponseEntity.ok(rules);
    }
    
    @PostMapping("/rules")
    public ResponseEntity<AuthorizationRule> createRule(@RequestBody AuthorizationRuleDto dto) {
        AuthorizationRule rule = ruleService.createRule(dto);
        return ResponseEntity.ok(rule);
    }
    
    @PutMapping("/rules/{ruleCode}")
    public ResponseEntity<AuthorizationRule> updateRule(
            @PathVariable String ruleCode, 
            @RequestBody AuthorizationRuleDto dto) {
        return ruleService.updateRule(ruleCode, dto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/rules/{ruleCode}")
    public ResponseEntity<Void> deleteRule(@PathVariable String ruleCode) {
        if (ruleService.deleteRule(ruleCode)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
    
    @PostMapping("/rules/{ruleCode}/toggle")
    public ResponseEntity<AuthorizationRule> toggleRuleStatus(@PathVariable String ruleCode) {
        return ruleService.toggleRuleStatus(ruleCode)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
