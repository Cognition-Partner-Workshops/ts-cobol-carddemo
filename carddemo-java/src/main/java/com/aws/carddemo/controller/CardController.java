package com.aws.carddemo.controller;

import com.aws.carddemo.dto.CardDto;
import com.aws.carddemo.service.CardService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping("/{cardNum}")
    public ResponseEntity<CardDto> getCard(@PathVariable String cardNum) {
        return ResponseEntity.ok(cardService.getCard(cardNum));
    }

    @GetMapping("/{cardNum}/with-account")
    public ResponseEntity<CardDto> getCardWithAccount(@PathVariable String cardNum) {
        return ResponseEntity.ok(cardService.getCardWithAccount(cardNum));
    }

    @GetMapping("/{cardNum}/with-transactions")
    public ResponseEntity<CardDto> getCardWithTransactions(@PathVariable String cardNum) {
        return ResponseEntity.ok(cardService.getCardWithTransactions(cardNum));
    }

    @GetMapping
    public ResponseEntity<Page<CardDto>> getAllCards(Pageable pageable) {
        return ResponseEntity.ok(cardService.getAllCards(pageable));
    }

    @GetMapping("/account/{acctId}")
    public ResponseEntity<List<CardDto>> getCardsByAccount(@PathVariable Long acctId) {
        return ResponseEntity.ok(cardService.getCardsByAccount(acctId));
    }

    @GetMapping("/active")
    public ResponseEntity<Page<CardDto>> getActiveCards(Pageable pageable) {
        return ResponseEntity.ok(cardService.getActiveCards(pageable));
    }

    @GetMapping("/expired")
    public ResponseEntity<List<CardDto>> getExpiredCards() {
        return ResponseEntity.ok(cardService.getExpiredCards());
    }

    @GetMapping("/expiring")
    public ResponseEntity<List<CardDto>> getCardsExpiringBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(cardService.getCardsExpiringBetween(startDate, endDate));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardDto> createCard(@Valid @RequestBody CardDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(dto));
    }

    @PutMapping("/{cardNum}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardDto> updateCard(@PathVariable String cardNum, @Valid @RequestBody CardDto dto) {
        return ResponseEntity.ok(cardService.updateCard(cardNum, dto));
    }

    @PatchMapping("/{cardNum}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateCard(@PathVariable String cardNum) {
        cardService.deactivateCard(cardNum);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count/active")
    public ResponseEntity<Long> countActiveCards() {
        return ResponseEntity.ok(cardService.countActiveCards());
    }
}
