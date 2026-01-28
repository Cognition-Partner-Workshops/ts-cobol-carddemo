package com.carddemo.card.controller;

import com.carddemo.card.dto.CreateCardRequest;
import com.carddemo.card.dto.UpdateCardRequest;
import com.carddemo.card.service.CardService;
import com.carddemo.common.dto.ApiResponse;
import com.carddemo.common.dto.CardDto;
import com.carddemo.common.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cards")
@Tag(name = "Cards", description = "Credit card management endpoints")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping
    @Operation(summary = "Get all cards", description = "Get paginated list of all cards")
    public ResponseEntity<ApiResponse<PagedResponse<CardDto>>> getAllCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "cardNumber") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        PagedResponse<CardDto> cards = cardService.getAllCards(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(cards));
    }

    @GetMapping("/{cardNumber}")
    @Operation(summary = "Get card by number", description = "Get card details by card number")
    public ResponseEntity<ApiResponse<CardDto>> getCardByNumber(@PathVariable String cardNumber) {
        CardDto card = cardService.getCardByNumber(cardNumber);
        return ResponseEntity.ok(ApiResponse.success(card));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get cards by account", description = "Get all cards for an account")
    public ResponseEntity<ApiResponse<List<CardDto>>> getCardsByAccountId(@PathVariable Long accountId) {
        List<CardDto> cards = cardService.getCardsByAccountId(accountId);
        return ResponseEntity.ok(ApiResponse.success(cards));
    }

    @PostMapping
    @Operation(summary = "Create card", description = "Create a new credit card")
    public ResponseEntity<ApiResponse<CardDto>> createCard(@Valid @RequestBody CreateCardRequest request) {
        CardDto card = cardService.createCard(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Card created successfully", card));
    }

    @PutMapping("/{cardNumber}")
    @Operation(summary = "Update card", description = "Update an existing card")
    public ResponseEntity<ApiResponse<CardDto>> updateCard(
            @PathVariable String cardNumber,
            @Valid @RequestBody UpdateCardRequest request) {
        CardDto card = cardService.updateCard(cardNumber, request);
        return ResponseEntity.ok(ApiResponse.success("Card updated successfully", card));
    }

    @PostMapping("/{cardNumber}/activate")
    @Operation(summary = "Activate card", description = "Activate a card")
    public ResponseEntity<ApiResponse<CardDto>> activateCard(@PathVariable String cardNumber) {
        CardDto card = cardService.activateCard(cardNumber);
        return ResponseEntity.ok(ApiResponse.success("Card activated successfully", card));
    }

    @PostMapping("/{cardNumber}/deactivate")
    @Operation(summary = "Deactivate card", description = "Deactivate a card")
    public ResponseEntity<ApiResponse<CardDto>> deactivateCard(@PathVariable String cardNumber) {
        CardDto card = cardService.deactivateCard(cardNumber);
        return ResponseEntity.ok(ApiResponse.success("Card deactivated successfully", card));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active cards", description = "Get all active cards")
    public ResponseEntity<ApiResponse<List<CardDto>>> getActiveCards() {
        List<CardDto> cards = cardService.getActiveCards();
        return ResponseEntity.ok(ApiResponse.success(cards));
    }

    @GetMapping("/expired")
    @Operation(summary = "Get expired cards", description = "Get all expired cards")
    public ResponseEntity<ApiResponse<List<CardDto>>> getExpiredCards() {
        List<CardDto> cards = cardService.getExpiredCards();
        return ResponseEntity.ok(ApiResponse.success(cards));
    }

    @GetMapping("/expiring")
    @Operation(summary = "Get expiring cards", description = "Get cards expiring within specified days")
    public ResponseEntity<ApiResponse<List<CardDto>>> getExpiringCards(
            @RequestParam(defaultValue = "30") int daysAhead) {
        List<CardDto> cards = cardService.getExpiringCards(daysAhead);
        return ResponseEntity.ok(ApiResponse.success(cards));
    }

    @GetMapping("/search")
    @Operation(summary = "Search cards", description = "Search cards by last four digits")
    public ResponseEntity<ApiResponse<List<CardDto>>> searchByLastFourDigits(@RequestParam String lastFour) {
        List<CardDto> cards = cardService.searchByLastFourDigits(lastFour);
        return ResponseEntity.ok(ApiResponse.success(cards));
    }
}
