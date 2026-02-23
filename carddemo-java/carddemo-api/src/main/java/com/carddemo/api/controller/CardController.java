package com.carddemo.api.controller;

import com.carddemo.api.dto.CardResponse;
import com.carddemo.api.dto.CardUpdateRequest;
import com.carddemo.api.dto.PageResponse;
import com.carddemo.api.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Card REST controller.
 * Replaces CICS transactions CCLI (COCRDLIC), CCDL (COCRDSLC), and CCUP (COCRDUPC).
 *
 * COBOL → Java mapping:
 *   CCLI → GET  /api/cards           (Card List)
 *   CCDL → GET  /api/cards/{cardNum} (Card View)
 *   CCUP → PUT  /api/cards/{cardNum} (Card Update)
 */
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Card management (replaces CICS CCLI/CCDL/CCUP)")
public class CardController {

    private final CardService cardService;

    @GetMapping
    @Operation(summary = "List cards", description = "Paginated card listing, optionally filtered by account")
    public ResponseEntity<PageResponse<CardResponse>> listCards(
            @Parameter(description = "Filter by account ID") @RequestParam(required = false) Long accountId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(cardService.listCards(accountId,
                PageRequest.of(page, size, Sort.by("cardNum"))));
    }

    @GetMapping("/{cardNumber}")
    @Operation(summary = "View card details", description = "Retrieves card by number (replaces CCDL)")
    public ResponseEntity<CardResponse> getCard(
            @PathVariable String cardNumber) {
        return ResponseEntity.ok(cardService.getCard(cardNumber));
    }

    @PutMapping("/{cardNumber}")
    @Operation(summary = "Update card", description = "Updates card fields (replaces CCUP)")
    public ResponseEntity<CardResponse> updateCard(
            @PathVariable String cardNumber,
            @Valid @RequestBody CardUpdateRequest request) {
        return ResponseEntity.ok(cardService.updateCard(cardNumber, request));
    }
}
