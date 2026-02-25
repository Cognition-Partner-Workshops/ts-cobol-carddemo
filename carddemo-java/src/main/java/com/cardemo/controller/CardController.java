package com.cardemo.controller;

import com.cardemo.entity.Card;
import com.cardemo.service.CardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Card management controller.
 * Migrated from CCLI (COCRDLIC - list), CCDL (COCRDSLC - detail), CCUP (COCRDUPC - update).
 */
@RestController
@RequestMapping("/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    /**
     * GET /cards?accountId=... - Migrated from CCLI (COCRDLIC) card list screen.
     */
    @GetMapping
    public ResponseEntity<List<Card>> getCards(@RequestParam("accountId") Long accountId) {
        return ResponseEntity.ok(cardService.getCardsByAccountId(accountId));
    }

    /**
     * GET /cards/{cardNum} - Migrated from CCDL (COCRDSLC) card detail screen.
     */
    @GetMapping("/{cardNum}")
    public ResponseEntity<Card> getCard(@PathVariable("cardNum") String cardNum) {
        return ResponseEntity.ok(cardService.getCard(cardNum));
    }

    /**
     * PUT /cards/{cardNum} - Migrated from CCUP (COCRDUPC) card update screen.
     */
    @PutMapping("/{cardNum}")
    public ResponseEntity<Card> updateCard(@PathVariable("cardNum") String cardNum, @RequestBody Card card) {
        return ResponseEntity.ok(cardService.updateCard(cardNum, card));
    }
}
