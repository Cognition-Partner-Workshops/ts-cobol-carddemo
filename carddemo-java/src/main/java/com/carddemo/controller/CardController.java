package com.carddemo.controller;

import com.carddemo.dto.CardDto;
import com.carddemo.entity.Card;
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

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping("/list")
    public ResponseEntity<Page<Card>> listCards(
            @RequestParam(required = false) Long acctId,
            @PageableDefault(size = 10) Pageable pageable) {
        if (acctId != null) {
            return ResponseEntity.ok(cardService.listCardsByAccount(acctId, pageable));
        }
        return ResponseEntity.ok(cardService.listCards(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardDto> viewCard(@PathVariable("id") String cardNum) {
        Card card = cardService.getCard(cardNum);
        return ResponseEntity.ok(cardService.toDto(card));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Card> updateCard(@PathVariable("id") String cardNum,
                                           @RequestBody CardDto cardDto) {
        Card updated = cardService.updateCard(cardNum, cardDto);
        return ResponseEntity.ok(updated);
    }
}
