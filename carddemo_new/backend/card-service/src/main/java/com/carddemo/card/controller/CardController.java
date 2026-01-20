package com.carddemo.card.controller;

import com.carddemo.card.dto.CardDto;
import com.carddemo.card.dto.CardUpdateRequest;
import com.carddemo.card.service.CardService;
import com.carddemo.common.dto.ApiResponse;
import com.carddemo.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Card Management", description = "Card list, view, and update operations - EPIC-003")
public class CardController {

    private final CardService cardService;

    @GetMapping("/{cardNumber}")
    @Operation(summary = "View card details", description = "Display single card details (US-003-02-01)")
    public ResponseEntity<ApiResponse<CardDto>> getCard(@PathVariable String cardNumber) {
        CardDto card = cardService.getCardByNumber(cardNumber);
        return ResponseEntity.ok(ApiResponse.success(card));
    }

    @GetMapping
    @Operation(summary = "List all cards", description = "Get paginated list of all cards (US-003-01-01)")
    public ResponseEntity<ApiResponse<PageResponse<CardDto>>> getAllCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<CardDto> cards = cardService.getAllCards(page, size);
        return ResponseEntity.ok(ApiResponse.success(cards));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "List cards by account", description = "Get cards for a specific account with pagination (US-003-01-02)")
    public ResponseEntity<ApiResponse<PageResponse<CardDto>>> getCardsByAccount(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<CardDto> cards = cardService.getCardsByAccount(accountId, page, size);
        return ResponseEntity.ok(ApiResponse.success(cards));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "List cards by customer", description = "Get all cards for a specific customer")
    public ResponseEntity<ApiResponse<List<CardDto>>> getCardsByCustomer(@PathVariable String customerId) {
        List<CardDto> cards = cardService.getCardsByCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success(cards));
    }

    @PutMapping("/{cardNumber}")
    @Operation(summary = "Update card", description = "Modify card details including embossed name and status (US-003-03-01 to US-003-03-05)")
    public ResponseEntity<ApiResponse<CardDto>> updateCard(
            @PathVariable String cardNumber,
            @Valid @RequestBody CardUpdateRequest request) {
        CardDto card = cardService.updateCard(cardNumber, request);
        return ResponseEntity.ok(ApiResponse.success(card, "Card updated successfully"));
    }
}
