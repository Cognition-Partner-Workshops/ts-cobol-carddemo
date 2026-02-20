package com.carddemo.service;

import com.carddemo.entity.Card;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.repository.CardRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardService {

    private final CardRepository cardRepository;

    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public Page<Card> listCards(Pageable pageable) {
        return cardRepository.findAll(pageable);
    }

    public Page<Card> listCardsByAccount(Long acctId, Pageable pageable) {
        return cardRepository.findByAcctId(acctId, pageable);
    }

    public Card getCard(String cardNum) {
        return cardRepository.findById(cardNum)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardNum));
    }

    @Transactional
    public Card updateCard(String cardNum, Card updatedCard) {
        Card existing = cardRepository.findById(cardNum)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardNum));

        if (updatedCard.getEmbossedName() != null) {
            existing.setEmbossedName(updatedCard.getEmbossedName());
        }
        if (updatedCard.getExpirationDate() != null) {
            existing.setExpirationDate(updatedCard.getExpirationDate());
        }
        if (updatedCard.getActiveStatus() != null) {
            existing.setActiveStatus(updatedCard.getActiveStatus());
        }

        return cardRepository.save(existing);
    }
}
