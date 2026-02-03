package com.aws.cardemo.services;

import com.aws.cardemo.domain.entity.Card;
import com.aws.cardemo.persistence.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CardService {

    private final CardRepository cardRepository;

    public List<Card> getAllCards() {
        return cardRepository.findAll();
    }

    public Optional<Card> getCardByNumber(String cardNumber) {
        return cardRepository.findById(cardNumber);
    }

    public Card createCard(Card card) {
        return cardRepository.save(card);
    }

    public Card updateCard(Card card) {
        return cardRepository.save(card);
    }

    public void deleteCard(String cardNumber) {
        cardRepository.deleteById(cardNumber);
    }

    public List<Card> getCardsByAccountId(String accountId) {
        return cardRepository.findByAccountId(accountId);
    }

    public List<Card> getActiveCardsByAccountId(String accountId) {
        return cardRepository.findActiveCardsByAccountId(accountId);
    }

    public List<Card> getCardsByStatus(String status) {
        return cardRepository.findByCardActiveStatus(status);
    }

    public Card activateCard(String cardNumber) {
        Card card = cardRepository.findById(cardNumber)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardNumber));
        card.setCardActiveStatus("Y");
        return cardRepository.save(card);
    }

    public Card deactivateCard(String cardNumber) {
        Card card = cardRepository.findById(cardNumber)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardNumber));
        card.setCardActiveStatus("N");
        return cardRepository.save(card);
    }

    public List<Card> searchByEmbossedName(String name) {
        return cardRepository.findByEmbossedNameContaining(name);
    }
}
