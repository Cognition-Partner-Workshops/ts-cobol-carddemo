package com.carddemo.service;

import com.carddemo.exception.BadRequestException;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.model.Card;
import com.carddemo.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {
    
    private final CardRepository cardRepository;
    
    public List<Card> getAllCards() {
        return cardRepository.findAll();
    }
    
    public Card getCardByNumber(String cardNumber) {
        return cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "cardNumber", cardNumber));
    }
    
    public List<Card> getCardsByAccountId(String accountId) {
        return cardRepository.findByAccountId(accountId);
    }
    
    public Card createCard(Card card) {
        if (cardRepository.existsByCardNumber(card.getCardNumber())) {
            throw new BadRequestException("Card with number " + card.getCardNumber() + " already exists");
        }
        return cardRepository.save(card);
    }
    
    public Card updateCard(String cardNumber, Card cardDetails) {
        Card card = getCardByNumber(cardNumber);
        
        card.setAccountId(cardDetails.getAccountId());
        card.setCvvCode(cardDetails.getCvvCode());
        card.setEmbossedName(cardDetails.getEmbossedName());
        card.setExpirationDate(cardDetails.getExpirationDate());
        card.setActiveStatus(cardDetails.getActiveStatus());
        
        return cardRepository.save(card);
    }
    
    public void deleteCard(String cardNumber) {
        if (!cardRepository.existsByCardNumber(cardNumber)) {
            throw new ResourceNotFoundException("Card", "cardNumber", cardNumber);
        }
        cardRepository.deleteByCardNumber(cardNumber);
    }
}
