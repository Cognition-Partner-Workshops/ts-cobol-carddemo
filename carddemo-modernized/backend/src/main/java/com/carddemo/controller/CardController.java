package com.carddemo.controller;

import com.carddemo.dto.ApiResponse;
import com.carddemo.model.Card;
import com.carddemo.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {
    
    private final CardService cardService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<Card>>> getAllCards() {
        List<Card> cards = cardService.getAllCards();
        return ResponseEntity.ok(ApiResponse.success(cards));
    }
    
    @GetMapping("/{cardNumber}")
    public ResponseEntity<ApiResponse<Card>> getCardByNumber(@PathVariable String cardNumber) {
        Card card = cardService.getCardByNumber(cardNumber);
        return ResponseEntity.ok(ApiResponse.success(card));
    }
    
    @GetMapping("/account/{accountId}")
    public ResponseEntity<ApiResponse<List<Card>>> getCardsByAccountId(@PathVariable String accountId) {
        List<Card> cards = cardService.getCardsByAccountId(accountId);
        return ResponseEntity.ok(ApiResponse.success(cards));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<Card>> createCard(@RequestBody Card card) {
        Card createdCard = cardService.createCard(card);
        return ResponseEntity.ok(ApiResponse.success("Card created successfully", createdCard));
    }
    
    @PutMapping("/{cardNumber}")
    public ResponseEntity<ApiResponse<Card>> updateCard(
            @PathVariable String cardNumber,
            @RequestBody Card cardDetails) {
        Card updatedCard = cardService.updateCard(cardNumber, cardDetails);
        return ResponseEntity.ok(ApiResponse.success("Card updated successfully", updatedCard));
    }
    
    @DeleteMapping("/{cardNumber}")
    public ResponseEntity<ApiResponse<Void>> deleteCard(@PathVariable String cardNumber) {
        cardService.deleteCard(cardNumber);
        return ResponseEntity.ok(ApiResponse.success("Card deleted successfully", null));
    }
}
