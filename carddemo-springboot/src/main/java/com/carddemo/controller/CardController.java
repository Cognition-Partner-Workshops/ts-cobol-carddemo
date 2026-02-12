package com.carddemo.controller;

import com.carddemo.service.CardService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for credit card management.
 * Replaces CICS card management transactions:
 * - COCRDLIC.cbl (Card List)
 * - COCRDSLC.cbl (Card Detail)
 */
@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }
}
