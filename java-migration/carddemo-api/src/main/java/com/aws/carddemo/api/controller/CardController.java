package com.aws.carddemo.api.controller;

import com.aws.carddemo.service.card.CardService;
import com.aws.carddemo.service.dto.CardDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Card management endpoints - migrated from COCRDLIC (CCLI), COCRDSLC, COCRDUPC")
public class CardController {

    private final CardService cardService;

    @GetMapping
    @Operation(summary = "List all cards with pagination")
    public ResponseEntity<Page<CardDTO>> listAllCards(Pageable pageable) {
        return ResponseEntity.ok(cardService.listAllCards(pageable));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "List cards by account")
    public ResponseEntity<Page<CardDTO>> listCards(@PathVariable Long accountId, Pageable pageable) {
        return ResponseEntity.ok(cardService.listCards(accountId, pageable));
    }

    @GetMapping("/{cardNumber}")
    @Operation(summary = "Get card by card number")
    public ResponseEntity<CardDTO> getCard(@PathVariable String cardNumber) {
        return cardService.getCard(cardNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create new card")
    public ResponseEntity<CardDTO> createCard(@Valid @RequestBody CardService.CardCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(request));
    }

    @PutMapping("/{cardNumber}")
    @Operation(summary = "Update card")
    public ResponseEntity<CardDTO> updateCard(
            @PathVariable String cardNumber,
            @Valid @RequestBody CardService.CardUpdateRequest request) {
        return ResponseEntity.ok(cardService.updateCard(cardNumber, request));
    }

    @GetMapping("/expiring")
    @Operation(summary = "Find cards expiring within specified days")
    public ResponseEntity<List<CardDTO>> findExpiringCards(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(cardService.findExpiringCards(java.time.LocalDate.now().plusDays(days)));
    }

    @GetMapping("/expired-active")
    @Operation(summary = "Find expired but still active cards")
    public ResponseEntity<List<CardDTO>> findExpiredActiveCards() {
        return ResponseEntity.ok(cardService.findExpiredActiveCards());
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get card statistics")
    public ResponseEntity<CardService.CardStatistics> getStatistics() {
        return ResponseEntity.ok(cardService.getStatistics());
    }
}
