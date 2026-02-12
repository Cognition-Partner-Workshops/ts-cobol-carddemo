package com.aws.carddemo.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.aws.carddemo.account.Account;
import com.aws.carddemo.card.dto.CardXrefResponse;
import com.aws.carddemo.customer.Customer;
import com.aws.carddemo.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class CardXrefServiceTests {

    @Mock
    private CardXrefRepository cardXrefRepository;

    @InjectMocks
    private CardXrefService cardXrefService;

    private CardXref testXref;
    private Account testAccount;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer();
        testCustomer.setId(1L);
        testCustomer.setFirstName("John");
        testCustomer.setLastName("Doe");

        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setAccountStatus("A");
        testAccount.setCreditLimit(new BigDecimal("5000.00"));
        testAccount.setCurrentBalance(BigDecimal.ZERO);
        testAccount.setOpenDate(LocalDate.of(2020, 1, 1));
        testAccount.setExpirationDate(LocalDate.of(2030, 12, 31));

        testXref = new CardXref();
        testXref.setCardNumber("4111111111111111");
        testXref.setAccount(testAccount);
        testXref.setCustomer(testCustomer);
    }

    @Test
    void findByCardNumber_returnsXref() {
        when(cardXrefRepository.findById("4111111111111111")).thenReturn(Optional.of(testXref));

        CardXrefResponse result = cardXrefService.findByCardNumber("4111111111111111");

        assertThat(result.cardNumber()).isEqualTo("4111111111111111");
        assertThat(result.accountId()).isEqualTo(1L);
        assertThat(result.accountStatus()).isEqualTo("A");
        assertThat(result.customerId()).isEqualTo(1L);
        assertThat(result.customerFirstName()).isEqualTo("John");
        assertThat(result.customerLastName()).isEqualTo("Doe");
    }

    @Test
    void findByCardNumber_notFound_throwsException() {
        when(cardXrefRepository.findById("9999999999999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardXrefService.findByCardNumber("9999999999999999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Card cross-reference not found");
    }

    @Test
    void findByAccountId_returnsXrefs() {
        when(cardXrefRepository.findByAccountId(1L)).thenReturn(List.of(testXref));

        List<CardXrefResponse> results = cardXrefService.findByAccountId(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).cardNumber()).isEqualTo("4111111111111111");
    }

    @Test
    void findByAccountId_notFound_throwsException() {
        when(cardXrefRepository.findByAccountId(999L)).thenReturn(List.of());

        assertThatThrownBy(() -> cardXrefService.findByAccountId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No card cross-references found for account");
    }

    @Test
    void findByCustomerId_returnsXrefs() {
        when(cardXrefRepository.findByCustomerId(1L)).thenReturn(List.of(testXref));

        List<CardXrefResponse> results = cardXrefService.findByCustomerId(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).customerFirstName()).isEqualTo("John");
    }

    @Test
    void findByCustomerId_notFound_throwsException() {
        when(cardXrefRepository.findByCustomerId(999L)).thenReturn(List.of());

        assertThatThrownBy(() -> cardXrefService.findByCustomerId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No card cross-references found for customer");
    }

    @Test
    void findByAccountId_multipleXrefs_returnsAll() {
        CardXref secondXref = new CardXref();
        secondXref.setCardNumber("5222222222222222");
        secondXref.setAccount(testAccount);
        secondXref.setCustomer(testCustomer);

        when(cardXrefRepository.findByAccountId(1L)).thenReturn(List.of(testXref, secondXref));

        List<CardXrefResponse> results = cardXrefService.findByAccountId(1L);

        assertThat(results).hasSize(2);
    }
}
