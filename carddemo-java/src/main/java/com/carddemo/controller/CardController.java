package com.carddemo.controller;

import com.carddemo.entity.Card;
import com.carddemo.service.CardService;
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

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping
    public ResponseEntity<Page<Card>> listCards(
            @RequestParam(required = false) Long acctId,
            Pageable pageable) {
        if (acctId != null) {
            return ResponseEntity.ok(cardService.listCardsByAccount(acctId, pageable));
        }
        return ResponseEntity.ok(cardService.listCards(pageable));
    }

    @GetMapping("/{cardNum}")
    public ResponseEntity<Card> getCard(@PathVariable String cardNum) {
        return ResponseEntity.ok(cardService.getCard(cardNum));
    }

    @PutMapping("/{cardNum}")
    public ResponseEntity<Card> updateCard(@PathVariable String cardNum,
                                           @RequestBody Card card) {
        return ResponseEntity.ok(cardService.updateCard(cardNum, card));
    }
}
