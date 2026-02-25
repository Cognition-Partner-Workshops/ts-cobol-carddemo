package com.carddemo.service;

import com.carddemo.entity.Card;
import com.carddemo.entity.CardAccountXref;
import com.carddemo.repository.CardAccountXrefRepository;
import com.carddemo.repository.CardRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Card service - migrated from:
 *   COCRDLIC (CCLI - Credit Card List)
 *   COCRDSLC (CCDL - Credit Card View)
 *   COCRDUPC (CCUP - Credit Card Update)
 *
 * COCRDLIC logic: Browse CARDAIX (card file by account AIX) to list cards for an account.
 * COCRDSLC logic: Read CARDDAT by card number key, display card details.
 * COCRDUPC logic: Read card, allow field edits, REWRITE card record.
 */
@Service
public class CardService {

    private final CardRepository cardRepository;
    private final CardAccountXrefRepository xrefRepository;

    public CardService(CardRepository cardRepository, CardAccountXrefRepository xrefRepository) {
        this.cardRepository = cardRepository;
        this.xrefRepository = xrefRepository;
    }

    /**
     * List cards for an account - migrated from COCRDLIC STARTBR/READNEXT on CARDAIX.
     */
    public Page<Card> listCardsByAccount(Long acctId, Pageable pageable) {
        return cardRepository.findByAcctId(acctId, pageable);
    }

    /**
     * View card details - migrated from COCRDSLC READ on CARDDAT.
     */
    public Optional<Card> viewCard(String cardNum) {
        return cardRepository.findById(cardNum);
    }

    /**
     * Update card - migrated from COCRDUPC REWRITE on CARDDAT.
     */
    @Transactional
    public Card updateCard(String cardNum, Card updatedData) {
        Card existing = cardRepository.findById(cardNum)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        if (updatedData.getEmbossedName() != null) {
            existing.setEmbossedName(updatedData.getEmbossedName());
        }
        if (updatedData.getExpirationDate() != null) {
            existing.setExpirationDate(updatedData.getExpirationDate());
        }
        if (updatedData.getActiveStatus() != null) {
            existing.setActiveStatus(updatedData.getActiveStatus());
        }

        return cardRepository.save(existing);
    }

    /**
     * Get cross-reference entries for a card number.
     */
    public List<CardAccountXref> getXrefByCard(String cardNum) {
        return xrefRepository.findByCardNum(cardNum);
    }

    /**
     * Get cross-reference entries for an account.
     */
    public List<CardAccountXref> getXrefByAccount(Long acctId) {
        return xrefRepository.findByAcctId(acctId);
    }
}
