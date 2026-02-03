package com.carddemo.service;

import com.carddemo.exception.BadRequestException;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.model.CardXref;
import com.carddemo.repository.CardXrefRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardXrefService {
    
    private final CardXrefRepository cardXrefRepository;
    
    public List<CardXref> getAllCardXrefs() {
        return cardXrefRepository.findAll();
    }
    
    public CardXref getCardXrefByCardNumber(String cardNumber) {
        return cardXrefRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new ResourceNotFoundException("CardXref", "cardNumber", cardNumber));
    }
    
    public List<CardXref> getCardXrefsByCustomerId(String customerId) {
        return cardXrefRepository.findByCustomerId(customerId);
    }
    
    public List<CardXref> getCardXrefsByAccountId(String accountId) {
        return cardXrefRepository.findByAccountId(accountId);
    }
    
    public CardXref createCardXref(CardXref cardXref) {
        if (cardXrefRepository.existsByCardNumber(cardXref.getCardNumber())) {
            throw new BadRequestException("CardXref with card number " + cardXref.getCardNumber() + " already exists");
        }
        return cardXrefRepository.save(cardXref);
    }
    
    public CardXref updateCardXref(String cardNumber, CardXref cardXrefDetails) {
        CardXref cardXref = getCardXrefByCardNumber(cardNumber);
        
        cardXref.setCustomerId(cardXrefDetails.getCustomerId());
        cardXref.setAccountId(cardXrefDetails.getAccountId());
        
        return cardXrefRepository.save(cardXref);
    }
    
    public void deleteCardXref(String cardNumber) {
        CardXref cardXref = getCardXrefByCardNumber(cardNumber);
        cardXrefRepository.delete(cardXref);
    }
}
