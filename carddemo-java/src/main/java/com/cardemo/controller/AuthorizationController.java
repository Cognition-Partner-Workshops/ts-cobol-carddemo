package com.cardemo.controller;

import com.cardemo.entity.AuthDetail;
import com.cardemo.entity.AuthFraud;
import com.cardemo.entity.AuthSummary;
import com.cardemo.service.AuthorizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Authorization controller (Optional IMS/DB2/MQ module).
 * Migrated from CPVS (COPAUS0C - list) and CPVD (COPAUS1C - detail/fraud).
 */
@RestController
@RequestMapping("/authorizations")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    public AuthorizationController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    /**
     * GET /authorizations?cardNum=... - Migrated from CPVS (COPAUS0C).
     */
    @GetMapping
    public ResponseEntity<List<AuthSummary>> getAuthorizations(@RequestParam("cardNum") String cardNum) {
        return ResponseEntity.ok(authorizationService.getAuthorizationsByCardNum(cardNum));
    }

    /**
     * GET /authorizations/{id} - Migrated from CPVD (COPAUS1C) detail view.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getAuthorization(@PathVariable("id") Long id) {
        AuthSummary summary = authorizationService.getAuthorizationSummary(id);
        List<AuthDetail> details = authorizationService.getAuthorizationDetails(id);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", summary);
        response.put("details", details);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /authorizations/{id}/fraud - Migrated from CPVD (COPAUS1C) fraud flagging.
     * Flags an authorization detail as fraudulent and inserts into AUTHFRDS table.
     */
    @PutMapping("/{id}/fraud")
    public ResponseEntity<AuthFraud> flagAsFraud(
            @PathVariable("id") Long authDetailId,
            @RequestParam(value = "fraudFlag", defaultValue = "Y") String fraudFlag) {
        return ResponseEntity.ok(authorizationService.flagAsFraud(authDetailId, fraudFlag));
    }
}
