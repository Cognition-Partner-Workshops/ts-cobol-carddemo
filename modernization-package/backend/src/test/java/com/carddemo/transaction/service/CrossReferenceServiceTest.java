package com.carddemo.transaction.service;

import com.carddemo.transaction.dto.CrossReferenceResponse;
import com.carddemo.transaction.entity.CardCrossReference;
import com.carddemo.transaction.exception.ResourceNotFoundException;
import com.carddemo.transaction.repository.CardCrossReferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CrossReferenceService.
 * Covers BR-AT-04 (Account/Card Must Exist) and BR-AT-05 (Cross-Reference Resolution).
 */
@ExtendWith(MockitoExtension.class)
class CrossReferenceServiceTest {

    @Mock
    private CardCrossReferenceRepository xrefRepository;

    @InjectMocks
    private CrossReferenceService crossReferenceService;

    private CardCrossReference sampleXref;

    @BeforeEach
    void setUp() {
        sampleXref = new CardCrossReference();
        sampleXref.setCardNumber("4111111111111111");
        sampleXref.setAccountId(new BigDecimal("1"));
        sampleXref.setCustomerId(new BigDecimal("100000001"));
    }

    @Nested
    @DisplayName("Path A: Account ID -> Card Number (BR-AT-04, BR-AT-05)")
    class PathATests {

        @Test
        @DisplayName("BR-AT-05: Valid account ID resolves to card number")
        void testResolve_ByAccountId_ReturnsCardNumber() {
            when(xrefRepository.findFirstByAccountId(new BigDecimal("1")))
                    .thenReturn(Optional.of(sampleXref));

            CrossReferenceResponse response = crossReferenceService.resolve("1", null);

            assertNotNull(response);
            assertEquals("4111111111111111", response.getCardNumber());
            assertEquals("00000000001", response.getAccountId());
            assertEquals(100000001L, response.getCustomerId());
        }

        @Test
        @DisplayName("BR-AT-04: Non-existent account ID throws ResourceNotFoundException")
        void testResolve_AccountNotFound() {
            when(xrefRepository.findFirstByAccountId(new BigDecimal("99999")))
                    .thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> crossReferenceService.resolve("99999", null));

            assertEquals("Account ID NOT found...", ex.getMessage());
            assertEquals("accountId", ex.getField());
            assertEquals("BR-AT-04", ex.getBusinessRule());
        }
    }

    @Nested
    @DisplayName("Path B: Card Number -> Account ID (BR-AT-04, BR-AT-05)")
    class PathBTests {

        @Test
        @DisplayName("BR-AT-05: Valid card number resolves to account ID")
        void testResolve_ByCardNumber_ReturnsAccountId() {
            when(xrefRepository.findByCardNumber("4111111111111111"))
                    .thenReturn(Optional.of(sampleXref));

            CrossReferenceResponse response = crossReferenceService.resolve(null, "4111111111111111");

            assertNotNull(response);
            assertEquals("4111111111111111", response.getCardNumber());
            assertEquals("00000000001", response.getAccountId());
            assertEquals(100000001L, response.getCustomerId());
        }

        @Test
        @DisplayName("BR-AT-04: Non-existent card number throws ResourceNotFoundException")
        void testResolve_CardNotFound() {
            when(xrefRepository.findByCardNumber("9999999999999999"))
                    .thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> crossReferenceService.resolve(null, "9999999999999999"));

            assertEquals("Card Number NOT found...", ex.getMessage());
            assertEquals("cardNumber", ex.getField());
            assertEquals("BR-AT-04", ex.getBusinessRule());
        }
    }

    @Nested
    @DisplayName("Input Validation")
    class InputValidationTests {

        @Test
        @DisplayName("Both accountId and cardNumber provided throws IllegalArgumentException")
        void testResolve_BothProvided() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> crossReferenceService.resolve("1", "4111111111111111"));

            assertEquals("Provide either accountId or cardNumber, not both", ex.getMessage());
        }

        @Test
        @DisplayName("Neither accountId nor cardNumber provided throws IllegalArgumentException")
        void testResolve_NeitherProvided() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> crossReferenceService.resolve(null, null));

            assertEquals("Either accountId or cardNumber must be provided", ex.getMessage());
        }

        @Test
        @DisplayName("Both blank throws IllegalArgumentException")
        void testResolve_BothBlank() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> crossReferenceService.resolve("", ""));

            assertEquals("Either accountId or cardNumber must be provided", ex.getMessage());
        }

        @Test
        @DisplayName("One blank and one valid resolves correctly")
        void testResolve_OneBlankOneValid() {
            when(xrefRepository.findByCardNumber("4111111111111111"))
                    .thenReturn(Optional.of(sampleXref));

            CrossReferenceResponse response = crossReferenceService.resolve("", "4111111111111111");

            assertNotNull(response);
            assertEquals("4111111111111111", response.getCardNumber());
        }
    }
}
