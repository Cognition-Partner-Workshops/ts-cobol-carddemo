package com.carddemo.service;

import com.carddemo.entity.Card;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardService cardService;

    private Card testCard;

    @BeforeEach
    void setUp() {
        testCard = new Card();
        testCard.setCardNum("9680294154603697");
        testCard.setAcctId(1L);
        testCard.setCvvCd(747);
        testCard.setEmbossedName("Immanuel Kessler");
        testCard.setActiveStatus("Y");
    }

    @Test
    void getCardSuccess() {
        when(cardRepository.findById("9680294154603697")).thenReturn(Optional.of(testCard));

        Card result = cardService.getCard("9680294154603697");

        assertNotNull(result);
        assertEquals("9680294154603697", result.getCardNum());
        assertEquals("Immanuel Kessler", result.getEmbossedName());
    }

    @Test
    void getCardNotFound() {
        when(cardRepository.findById("0000000000000000")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> cardService.getCard("0000000000000000"));
    }

    @Test
    void updateCardSuccess() {
        when(cardRepository.findById("9680294154603697")).thenReturn(Optional.of(testCard));
        when(cardRepository.save(testCard)).thenReturn(testCard);

        Card update = new Card();
        update.setEmbossedName("Updated Name");

        Card result = cardService.updateCard("9680294154603697", update);

        assertNotNull(result);
        assertEquals("Updated Name", result.getEmbossedName());
    }
}
