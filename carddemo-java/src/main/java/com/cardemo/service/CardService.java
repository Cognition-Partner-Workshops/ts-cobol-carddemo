package com.cardemo.service;

import com.cardemo.entity.Card;
import com.cardemo.entity.CardAccountXref;
import com.cardemo.exception.CardDemoException;
import com.cardemo.repository.CardAccountXrefRepository;
import com.cardemo.repository.CardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Card management service.
 * Migrated from COCRDLIC (CCLI - list), COCRDSLC (CCDL - detail), COCRDUPC (CCUP - update).
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
     * List cards by account ID - migrated from COCRDLIC (CCLI transaction).
     * COBOL: EXEC CICS STARTBR DATASET(WS-CARDFILENAME) RIDFLD(WS-CARD-RID-ACCT-ID)
     * Uses XREFFILE to find cards belonging to an account.
     */
    public List<Card> getCardsByAccountId(Long accountId) {
        List<CardAccountXref> xrefs = xrefRepository.findByXrefAcctId(accountId);
        List<String> cardNums = xrefs.stream()
                .map(CardAccountXref::getXrefCardNum)
                .toList();
        return cardRepository.findAllById(cardNums);
    }

    /**
     * Get card detail - migrated from COCRDSLC (CCDL transaction).
     * COBOL: EXEC CICS READ DATASET(WS-CARDFILENAME) INTO(CARD-RECORD)
     */
    public Card getCard(String cardNum) {
        return cardRepository.findById(cardNum)
                .orElseThrow(() -> CardDemoException.notFound("Card not found: " + cardNum));
    }

    /**
     * Update card - migrated from COCRDUPC (CCUP transaction).
     * COBOL: EXEC CICS REWRITE DATASET(WS-CARDFILENAME) FROM(CARD-RECORD)
     * Validates: card status can only be 'Y' or 'N'
     */
    @Transactional
    public Card updateCard(String cardNum, Card updatedCard) {
        Card existing = cardRepository.findById(cardNum)
                .orElseThrow(() -> CardDemoException.notFound("Card not found: " + cardNum));

        // Validate card status - COBOL: IF FLG-ACTVSTS-NOT-OK
        if (updatedCard.getCardActiveStatus() != null) {
            String status = updatedCard.getCardActiveStatus().toUpperCase();
            if (!"Y".equals(status) && !"N".equals(status)) {
                throw CardDemoException.badRequest("Card status must be 'Y' (active) or 'N' (inactive)");
            }
            existing.setCardActiveStatus(status);
        }
        if (updatedCard.getCardEmbossedName() != null) {
            existing.setCardEmbossedName(updatedCard.getCardEmbossedName());
        }
        if (updatedCard.getCardExpirationDate() != null) {
            existing.setCardExpirationDate(updatedCard.getCardExpirationDate());
        }

        return cardRepository.save(existing);
    }
}
