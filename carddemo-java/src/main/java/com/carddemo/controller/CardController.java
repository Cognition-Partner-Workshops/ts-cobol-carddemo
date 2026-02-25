package com.carddemo.controller;

import com.carddemo.entity.Card;
import com.carddemo.entity.CardAccountXref;
import com.carddemo.service.CardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
 * Card controller - migrated from:
 *   COCRDLIC (CCLI - Credit Card List)
 *   COCRDSLC (CCDL - Credit Card View/Detail)
 *   COCRDUPC (CCUP - Credit Card Update)
 */
@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    /**
     * GET /api/cards?acctId={acctId} - List cards for an account (CCLI transaction).
     * Replaces COCRDLIC STARTBR/READNEXT on CARDAIX.
     */
    @GetMapping
    public ResponseEntity<Page<Card>> listCards(
            @RequestParam Long acctId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(cardService.listCardsByAccount(acctId, pageable));
    }

    /**
     * GET /api/cards/{cardNum} - View card detail (CCDL transaction).
     * Replaces COCRDSLC READ on CARDDAT.
     */
    @GetMapping("/{cardNum}")
    public ResponseEntity<Card> viewCard(@PathVariable String cardNum) {
        Card card = cardService.viewCard(cardNum)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        return ResponseEntity.ok(card);
    }

    /**
     * PUT /api/cards/{cardNum} - Update card (CCUP transaction).
     * Replaces COCRDUPC REWRITE on CARDDAT.
     */
    @PutMapping("/{cardNum}")
    public ResponseEntity<Card> updateCard(@PathVariable String cardNum, @RequestBody Card updatedData) {
        return ResponseEntity.ok(cardService.updateCard(cardNum, updatedData));
    }

    /**
     * GET /api/cards/{cardNum}/xref - Get cross-reference data for a card.
     */
    @GetMapping("/{cardNum}/xref")
    public ResponseEntity<List<CardAccountXref>> getXref(@PathVariable String cardNum) {
        return ResponseEntity.ok(cardService.getXrefByCard(cardNum));
    }
}
