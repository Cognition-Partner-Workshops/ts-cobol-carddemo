package com.aws.cardemo.api.controller;

import com.aws.cardemo.domain.entity.Card;
import com.aws.cardemo.services.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Tag(name = "Card", description = "Card management APIs")
public class CardController {

    private final CardService cardService;

    @GetMapping
    @Operation(summary = "Get all cards")
    public ResponseEntity<List<Card>> getAllCards() {
        return ResponseEntity.ok(cardService.getAllCards());
    }

    @GetMapping("/{cardNumber}")
    @Operation(summary = "Get card by number")
    public ResponseEntity<Card> getCardByNumber(@PathVariable String cardNumber) {
        return cardService.getCardByNumber(cardNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new card")
    public ResponseEntity<Card> createCard(@Valid @RequestBody Card card) {
        Card created = cardService.createCard(card);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{cardNumber}")
    @Operation(summary = "Update an existing card")
    public ResponseEntity<Card> updateCard(
            @PathVariable String cardNumber,
            @Valid @RequestBody Card card) {
        card.setCardNumber(cardNumber);
        Card updated = cardService.updateCard(card);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{cardNumber}")
    @Operation(summary = "Delete a card")
    public ResponseEntity<Void> deleteCard(@PathVariable String cardNumber) {
        cardService.deleteCard(cardNumber);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get cards by account ID")
    public ResponseEntity<List<Card>> getCardsByAccountId(@PathVariable String accountId) {
        return ResponseEntity.ok(cardService.getCardsByAccountId(accountId));
    }

    @GetMapping("/account/{accountId}/active")
    @Operation(summary = "Get active cards by account ID")
    public ResponseEntity<List<Card>> getActiveCardsByAccountId(@PathVariable String accountId) {
        return ResponseEntity.ok(cardService.getActiveCardsByAccountId(accountId));
    }

    @PostMapping("/{cardNumber}/activate")
    @Operation(summary = "Activate a card")
    public ResponseEntity<Card> activateCard(@PathVariable String cardNumber) {
        Card activated = cardService.activateCard(cardNumber);
        return ResponseEntity.ok(activated);
    }

    @PostMapping("/{cardNumber}/deactivate")
    @Operation(summary = "Deactivate a card")
    public ResponseEntity<Card> deactivateCard(@PathVariable String cardNumber) {
        Card deactivated = cardService.deactivateCard(cardNumber);
        return ResponseEntity.ok(deactivated);
    }
}
