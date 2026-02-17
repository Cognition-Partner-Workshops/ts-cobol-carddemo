package com.aws.carddemo.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.aws.carddemo.account.Account;
import com.aws.carddemo.card.dto.CardDetailResponse;
import com.aws.carddemo.card.dto.CardListItemResponse;
import com.aws.carddemo.card.dto.CardUpdateRequest;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.exception.ValidationException;

@ExtendWith(MockitoExtension.class)
class CardServiceTests {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardService cardService;

    private Card testCard;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setAccountStatus("A");
        testAccount.setCreditLimit(new BigDecimal("5000.00"));
        testAccount.setCurrentBalance(new BigDecimal("1200.00"));
        testAccount.setOpenDate(LocalDate.of(2020, 1, 15));
        testAccount.setExpirationDate(LocalDate.of(2030, 12, 31));

        testCard = new Card();
        testCard.setId(1L);
        testCard.setCardNumber("4111111111111111");
        testCard.setCardStatus("A");
        testCard.setEmbossedName("JOHN DOE");
        testCard.setCvvCode("123");
        testCard.setIssuedDate(LocalDate.of(2023, 1, 1));
        testCard.setExpiryDate(LocalDate.of(2028, 12, 31));
        testCard.setAccount(testAccount);
    }

    @Test
    void listCardsByAccount_returnsPagedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> cardPage = new PageImpl<>(List.of(testCard), pageable, 1);
        when(cardRepository.findByAccountId(1L, pageable)).thenReturn(cardPage);

        Page<CardListItemResponse> result = cardService.listCardsByAccount(1L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        CardListItemResponse item = result.getContent().get(0);
        assertThat(item.maskedCardNumber()).isEqualTo("************1111");
        assertThat(item.cardStatus()).isEqualTo("A");
        assertThat(item.embossedName()).isEqualTo("JOHN DOE");
    }

    @Test
    void listCardsByAccount_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(cardRepository.findByAccountId(999L, pageable)).thenReturn(emptyPage);

        Page<CardListItemResponse> result = cardService.listCardsByAccount(999L, pageable);

        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void getCardByNumber_returnsFullDetails() {
        when(cardRepository.findByCardNumber("4111111111111111")).thenReturn(Optional.of(testCard));

        CardDetailResponse result = cardService.getCardByNumber("4111111111111111");

        assertThat(result.cardNumber()).isEqualTo("4111111111111111");
        assertThat(result.accountId()).isEqualTo(1L);
        assertThat(result.cardStatus()).isEqualTo("A");
        assertThat(result.embossedName()).isEqualTo("JOHN DOE");
        assertThat(result.maskedCvv()).isEqualTo("***");
        assertThat(result.accountSummary()).isNotNull();
        assertThat(result.accountSummary().creditLimit()).isEqualTo(new BigDecimal("5000.00"));
    }

    @Test
    void getCardByNumber_notFound_throwsException() {
        when(cardRepository.findByCardNumber("4111111111111111")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getCardByNumber("4111111111111111"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Card not found");
    }

    @Test
    void getCardByNumber_invalidFormat_throwsValidation() {
        assertThatThrownBy(() -> cardService.getCardByNumber("123"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("16 digits");
    }

    @Test
    void getCardByNumber_nonNumeric_throwsValidation() {
        assertThatThrownBy(() -> cardService.getCardByNumber("411111111111ABCD"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("16 digits");
    }

    @Test
    void updateCard_updatesStatus() {
        when(cardRepository.findByCardNumber("4111111111111111")).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        CardUpdateRequest request = new CardUpdateRequest("C", null, null);
        CardDetailResponse result = cardService.updateCard("4111111111111111", request);

        assertThat(testCard.getCardStatus()).isEqualTo("C");
        verify(cardRepository).save(testCard);
    }

    @Test
    void updateCard_updatesEmbossedName() {
        when(cardRepository.findByCardNumber("4111111111111111")).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        CardUpdateRequest request = new CardUpdateRequest(null, "JANE DOE", null);
        cardService.updateCard("4111111111111111", request);

        assertThat(testCard.getEmbossedName()).isEqualTo("JANE DOE");
    }

    @Test
    void updateCard_updatesExpiryDate() {
        LocalDate futureDate = LocalDate.now().plusYears(3);
        when(cardRepository.findByCardNumber("4111111111111111")).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        CardUpdateRequest request = new CardUpdateRequest(null, null, futureDate);
        cardService.updateCard("4111111111111111", request);

        assertThat(testCard.getExpiryDate()).isEqualTo(futureDate);
    }

    @Test
    void updateCard_pastExpiryDate_throwsValidation() {
        when(cardRepository.findByCardNumber("4111111111111111")).thenReturn(Optional.of(testCard));

        LocalDate pastDate = LocalDate.now().minusDays(1);
        CardUpdateRequest request = new CardUpdateRequest(null, null, pastDate);

        assertThatThrownBy(() -> cardService.updateCard("4111111111111111", request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("future");

        verify(cardRepository, never()).save(any());
    }

    @Test
    void updateCard_reactivateCancelled_throwsValidation() {
        testCard.setCardStatus("C");
        when(cardRepository.findByCardNumber("4111111111111111")).thenReturn(Optional.of(testCard));

        CardUpdateRequest request = new CardUpdateRequest("A", null, null);

        assertThatThrownBy(() -> cardService.updateCard("4111111111111111", request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot reactivate");

        verify(cardRepository, never()).save(any());
    }

    @Test
    void updateCard_reactivateLost_throwsValidation() {
        testCard.setCardStatus("L");
        when(cardRepository.findByCardNumber("4111111111111111")).thenReturn(Optional.of(testCard));

        CardUpdateRequest request = new CardUpdateRequest("A", null, null);

        assertThatThrownBy(() -> cardService.updateCard("4111111111111111", request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot reactivate");
    }

    @Test
    void updateCard_blankEmbossedName_throwsValidation() {
        when(cardRepository.findByCardNumber("4111111111111111")).thenReturn(Optional.of(testCard));

        CardUpdateRequest request = new CardUpdateRequest(null, "   ", null);

        assertThatThrownBy(() -> cardService.updateCard("4111111111111111", request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void updateCard_cancelledToLost_succeeds() {
        testCard.setCardStatus("C");
        when(cardRepository.findByCardNumber("4111111111111111")).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        CardUpdateRequest request = new CardUpdateRequest("L", null, null);
        cardService.updateCard("4111111111111111", request);

        assertThat(testCard.getCardStatus()).isEqualTo("L");
    }
}
