package com.aws.carddemo.api.controller;

import com.aws.carddemo.service.authorization.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/authorizations")
@RequiredArgsConstructor
@Tag(name = "Authorizations", description = "Credit card authorization endpoints - migrated from COPAUA0C (CP00)")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    @PostMapping
    @Operation(summary = "Process credit card authorization with fraud detection")
    public ResponseEntity<AuthorizationService.AuthorizationResponse> authorize(
            @Valid @RequestBody AuthorizationService.AuthorizationRequest request) {
        return ResponseEntity.ok(authorizationService.authorize(request));
    }
}
