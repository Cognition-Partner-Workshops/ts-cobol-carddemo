package com.aws.carddemo.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CardTest {

    private Card card;

    @BeforeEach
    void setUp() {
        card = new Card();
        card.setCardNum("4111111111111111");
        card.setCardActiveStatus("Y");
        card.setCardExpirationDate(LocalDate.now().plusYears(1));
        card.setCardEmbossedName("JOHN DOE");
        card.setCardCvvCd("123");
    }

    @Nested
    @DisplayName("Card Status Tests")
    class CardStatusTests {

        @Test
        @DisplayName("isActive returns true for status Y")
        void isActive_StatusY_ReturnsTrue() {
            card.setCardActiveStatus("Y");
            assertTrue(card.isActive());
        }

        @Test
        @DisplayName("isActive returns false for status N")
        void isActive_StatusN_ReturnsFalse() {
            card.setCardActiveStatus("N");
            assertFalse(card.isActive());
        }

        @Test
        @DisplayName("isActive returns false for null status")
        void isActive_NullStatus_ReturnsFalse() {
            card.setCardActiveStatus(null);
            assertFalse(card.isActive());
        }

        @Test
        @DisplayName("isExpired returns false for future expiration date")
        void isExpired_FutureDate_ReturnsFalse() {
            card.setCardExpirationDate(LocalDate.now().plusDays(1));
            assertFalse(card.isExpired());
        }

        @Test
        @DisplayName("isExpired returns true for past expiration date")
        void isExpired_PastDate_ReturnsTrue() {
            card.setCardExpirationDate(LocalDate.now().minusDays(1));
            assertTrue(card.isExpired());
        }

        @Test
        @DisplayName("isExpired returns false for today's date")
        void isExpired_TodayDate_ReturnsFalse() {
            card.setCardExpirationDate(LocalDate.now());
            assertFalse(card.isExpired());
        }

        @Test
        @DisplayName("isExpired returns false for null expiration date")
        void isExpired_NullDate_ReturnsFalse() {
            card.setCardExpirationDate(null);
            assertFalse(card.isExpired());
        }
    }

    @Nested
    @DisplayName("Card Number Masking Tests")
    class CardNumberMaskingTests {

        @Test
        @DisplayName("getMaskedCardNumber masks all but last 4 digits")
        void getMaskedCardNumber_MasksCorrectly() {
            card.setCardNum("4111111111111111");

            String masked = card.getMaskedCardNumber();

            assertEquals("************1111", masked);
        }

        @Test
        @DisplayName("getMaskedCardNumber handles short card numbers")
        void getMaskedCardNumber_ShortNumber_ReturnsAllMasked() {
            card.setCardNum("1234");

            String masked = card.getMaskedCardNumber();

            assertEquals("1234", masked);
        }

        @Test
        @DisplayName("getMaskedCardNumber handles null card number")
        void getMaskedCardNumber_NullNumber_ReturnsNull() {
            card.setCardNum(null);

            String masked = card.getMaskedCardNumber();

            assertNull(masked);
        }

        @Test
        @DisplayName("getMaskedCardNumber handles 15-digit card numbers")
        void getMaskedCardNumber_15Digits_MasksCorrectly() {
            card.setCardNum("378282246310005");

            String masked = card.getMaskedCardNumber();

            assertEquals("***********0005", masked);
        }
    }
}
