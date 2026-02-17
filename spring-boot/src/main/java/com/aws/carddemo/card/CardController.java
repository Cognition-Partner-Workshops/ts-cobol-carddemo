package com.aws.carddemo.card;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aws.carddemo.card.dto.CardDetailResponse;
import com.aws.carddemo.card.dto.CardListItemResponse;
import com.aws.carddemo.card.dto.CardUpdateRequest;
import com.aws.carddemo.card.dto.CardXrefResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;
    private final CardXrefService cardXrefService;

    public CardController(CardService cardService, CardXrefService cardXrefService) {
        this.cardService = cardService;
        this.cardXrefService = cardXrefService;
    }

    @GetMapping
    public ResponseEntity<Page<CardListItemResponse>> listCards(
            @RequestParam Long accountId,
            Pageable pageable) {
        return ResponseEntity.ok(cardService.listCardsByAccount(accountId, pageable));
    }

    @GetMapping("/{cardNumber}")
    public ResponseEntity<CardDetailResponse> getCard(@PathVariable String cardNumber) {
        return ResponseEntity.ok(cardService.getCardByNumber(cardNumber));
    }

    @PutMapping("/{cardNumber}")
    public ResponseEntity<CardDetailResponse> updateCard(
            @PathVariable String cardNumber,
            @Valid @RequestBody CardUpdateRequest request) {
        return ResponseEntity.ok(cardService.updateCard(cardNumber, request));
    }

    @GetMapping("/xref")
    public ResponseEntity<CardXrefResponse> getCardXref(@RequestParam String cardNumber) {
        return ResponseEntity.ok(cardXrefService.findByCardNumber(cardNumber));
    }
}
