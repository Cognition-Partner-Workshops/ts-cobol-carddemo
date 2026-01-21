package com.carddemo.repository;

import com.carddemo.entity.Card;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CardRepositoryTest {

    @Autowired
    private CardRepository cardRepository;

    private Card testCard;

    @BeforeEach
    void setUp() {
        testCard = new Card();
        testCard.setCardNumber("4111111111111111");
        testCard.setAccountId(12345678901L);
        testCard.setCustomerId(123456789L);
        testCard.setCardholderName("John Doe");
        testCard.setExpirationDate(LocalDate.of(2025, 12, 31));
        testCard.setActiveStatus("Y");
    }

    @Test
    void testSaveAndFindById() {
        Card saved = cardRepository.save(testCard);
        
        Optional<Card> found = cardRepository.findById(saved.getCardNumber());
        
        assertTrue(found.isPresent());
        assertEquals(testCard.getCardholderName(), found.get().getCardholderName());
    }

    @Test
    void testFindByAccountId() {
        cardRepository.save(testCard);
        
        List<Card> cards = cardRepository.findByAccountId(12345678901L);
        
        assertEquals(1, cards.size());
        assertEquals(testCard.getCardNumber(), cards.get(0).getCardNumber());
    }

    @Test
    void testFindByCustomerId() {
        cardRepository.save(testCard);
        
        List<Card> cards = cardRepository.findByCustomerId(123456789L);
        
        assertEquals(1, cards.size());
    }

    @Test
    void testFindByCustomerIdWithPagination() {
        cardRepository.save(testCard);
        
        Page<Card> page = cardRepository.findByCustomerId(123456789L, PageRequest.of(0, 10));
        
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void testFindByActiveStatus() {
        cardRepository.save(testCard);
        
        List<Card> activeCards = cardRepository.findByActiveStatus("Y");
        
        assertEquals(1, activeCards.size());
    }

    @Test
    void testFindByExpirationDateBefore() {
        cardRepository.save(testCard);
        
        List<Card> cards = cardRepository.findByExpirationDateBefore(LocalDate.of(2026, 1, 1));
        
        assertEquals(1, cards.size());
    }

    @Test
    void testFindByExpirationDateBetween() {
        cardRepository.save(testCard);
        
        List<Card> cards = cardRepository.findByExpirationDateBetween(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31)
        );
        
        assertEquals(1, cards.size());
    }

    @Test
    void testFindByAccountIdAndActiveStatus() {
        cardRepository.save(testCard);
        
        List<Card> cards = cardRepository.findByAccountIdAndActiveStatus(12345678901L, "Y");
        
        assertEquals(1, cards.size());
    }

    @Test
    void testCountByActiveStatus() {
        cardRepository.save(testCard);
        
        long count = cardRepository.countByActiveStatus("Y");
        
        assertEquals(1, count);
    }

    @Test
    void testCountByAccountId() {
        cardRepository.save(testCard);
        
        long count = cardRepository.countByAccountId(12345678901L);
        
        assertEquals(1, count);
    }
}
