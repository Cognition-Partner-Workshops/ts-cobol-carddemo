package com.carddemo.controller;

import com.carddemo.dto.AuthorizationRequest;
import com.carddemo.entity.AuthorizationDetail;
import com.carddemo.entity.AuthorizationSummary;
import com.carddemo.service.AuthorizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/authorizations")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    public AuthorizationController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @PostMapping
    public ResponseEntity<AuthorizationDetail> processAuthorization(
            @Valid @RequestBody AuthorizationRequest request) {
        AuthorizationDetail detail = authorizationService.processAuthorization(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(detail);
    }

    @GetMapping("/summary")
    public ResponseEntity<AuthorizationSummary> getAuthorizationSummary(
            @RequestParam String cardNum) {
        return ResponseEntity.ok(authorizationService.getAuthorizationSummary(cardNum));
    }

    @GetMapping("/{id}")
    public ResponseEntity<List<AuthorizationDetail>> getAuthorizationDetails(
            @PathVariable("id") Long authId) {
        return ResponseEntity.ok(authorizationService.getAuthorizationDetails(authId));
    }
}
