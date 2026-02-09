package com.carddemo.service;

import com.carddemo.dto.CardDto;
import com.carddemo.entity.Card;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.repository.CardRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CardService {

    private final CardRepository cardRepository;

    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Transactional(readOnly = true)
    public Page<Card> listCards(Pageable pageable) {
        return cardRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Card> listCardsByAccount(Long acctId, Pageable pageable) {
        return cardRepository.findByAcctId(acctId, pageable);
    }

    @Transactional(readOnly = true)
    public Card getCard(String cardNum) {
        return cardRepository.findById(cardNum)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardNum));
    }

    public Card updateCard(String cardNum, CardDto cardDto) {
        Card existing = cardRepository.findById(cardNum)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardNum));

        if (cardDto.getEmbossedName() != null) {
            existing.setEmbossedName(cardDto.getEmbossedName());
        }
        if (cardDto.getExpirationDate() != null) {
            existing.setExpirationDate(cardDto.getExpirationDate());
        }
        if (cardDto.getActiveStatus() != null) {
            existing.setActiveStatus(cardDto.getActiveStatus());
        }

        return cardRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public CardDto toDto(Card card) {
        CardDto dto = new CardDto();
        dto.setCardNum(card.getCardNum());
        dto.setAcctId(card.getAcctId());
        dto.setCvvCd(card.getCvvCd());
        dto.setEmbossedName(card.getEmbossedName());
        dto.setExpirationDate(card.getExpirationDate());
        dto.setActiveStatus(card.getActiveStatus());
        return dto;
    }
}
