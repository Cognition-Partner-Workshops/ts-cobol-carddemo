package com.carddemo.controller;

import com.carddemo.entity.PendingAuthorization;
import com.carddemo.service.AuthorizationService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/authorizations")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    public AuthorizationController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ResponseEntity<Page<PendingAuthorization>> listAuthorizations(
            @RequestParam(required = false) Long acctId,
            Pageable pageable) {
        if (acctId != null) {
            return ResponseEntity.ok(authorizationService.listByAccount(acctId, pageable));
        }
        return ResponseEntity.ok(authorizationService.listPendingAuthorizations(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PendingAuthorization> getAuthorization(@PathVariable Long id) {
        return ResponseEntity.ok(authorizationService.getAuthorization(id));
    }

    @PostMapping
    public ResponseEntity<PendingAuthorization> processAuthorization(
            @RequestBody PendingAuthorization auth) {
        return ResponseEntity.ok(authorizationService.processAuthorization(auth));
    }

    @PutMapping("/{id}/fraud")
    public ResponseEntity<PendingAuthorization> markAsFraud(@PathVariable Long id) {
        return ResponseEntity.ok(authorizationService.markAsFraud(id));
    }

    @DeleteMapping("/expired")
    public ResponseEntity<Map<String, Object>> purgeExpired() {
        int count = authorizationService.purgeExpiredAuthorizations();
        return ResponseEntity.ok(Map.of("purged", count));
    }
}
