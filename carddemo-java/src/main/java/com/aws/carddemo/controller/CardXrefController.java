package com.aws.carddemo.controller;

import com.aws.carddemo.entity.CardXref;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.repository.CardXrefRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/card-xrefs")
public class CardXrefController {

    private final CardXrefRepository cardXrefRepository;

    public CardXrefController(CardXrefRepository cardXrefRepository) {
        this.cardXrefRepository = cardXrefRepository;
    }

    @GetMapping("/{cardNum}")
    public ResponseEntity<CardXref> getCardXref(@PathVariable String cardNum) {
        CardXref xref = cardXrefRepository.findById(cardNum)
                .orElseThrow(() -> new ResourceNotFoundException("CardXref", "cardNum", cardNum));
        return ResponseEntity.ok(xref);
    }

    @GetMapping("/{cardNum}/details")
    public ResponseEntity<CardXref> getCardXrefWithDetails(@PathVariable String cardNum) {
        CardXref xref = cardXrefRepository.findByCardNumWithDetails(cardNum)
                .orElseThrow(() -> new ResourceNotFoundException("CardXref", "cardNum", cardNum));
        return ResponseEntity.ok(xref);
    }

    @GetMapping
    public ResponseEntity<Page<CardXref>> getAllCardXrefs(Pageable pageable) {
        return ResponseEntity.ok(cardXrefRepository.findAll(pageable));
    }

    @GetMapping("/customer/{custId}")
    public ResponseEntity<List<CardXref>> getCardXrefsByCustomer(@PathVariable Long custId) {
        return ResponseEntity.ok(cardXrefRepository.findByCustomerCustId(custId));
    }

    @GetMapping("/account/{acctId}")
    public ResponseEntity<List<CardXref>> getCardXrefsByAccount(@PathVariable Long acctId) {
        return ResponseEntity.ok(cardXrefRepository.findByAccountAcctId(acctId));
    }
}
