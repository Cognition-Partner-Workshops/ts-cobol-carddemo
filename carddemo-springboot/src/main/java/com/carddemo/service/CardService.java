package com.carddemo.service;

import com.carddemo.repository.CardRepository;
import org.springframework.stereotype.Service;

/**
 * Business logic for credit card management.
 * Will contain migrated logic from:
 * - COCRDLIC.cbl (Card List)
 * - COCRDSLC.cbl (Card Detail)
 * - CBACT02C.cbl (Card batch processing)
 */
@Service
public class CardService {

    private final CardRepository cardRepository;

    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }
}
