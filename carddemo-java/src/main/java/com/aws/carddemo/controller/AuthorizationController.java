package com.aws.carddemo.controller;

import com.aws.carddemo.dto.AuthRequestDto;
import com.aws.carddemo.service.AuthorizationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/authorizations")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    public AuthorizationController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @PostMapping
    public ResponseEntity<AuthRequestDto> processAuthorization(@Valid @RequestBody AuthRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authorizationService.processAuthorizationRequest(dto));
    }

    @GetMapping("/{authId}")
    public ResponseEntity<AuthRequestDto> getAuthRequest(@PathVariable Long authId) {
        return ResponseEntity.ok(authorizationService.getAuthRequest(authId));
    }

    @GetMapping("/card/{cardNum}")
    public ResponseEntity<Page<AuthRequestDto>> getAuthRequestsByCard(@PathVariable String cardNum, Pageable pageable) {
        return ResponseEntity.ok(authorizationService.getAuthRequestsByCard(cardNum, pageable));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<AuthRequestDto>> getAuthRequestsByStatus(@PathVariable String status, Pageable pageable) {
        return ResponseEntity.ok(authorizationService.getAuthRequestsByStatus(status, pageable));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<AuthRequestDto>> getPendingAuthRequests() {
        return ResponseEntity.ok(authorizationService.getPendingAuthRequests());
    }
}
