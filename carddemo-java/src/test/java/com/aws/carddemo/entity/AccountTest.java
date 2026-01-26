package com.aws.carddemo.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setAcctId(12345678901L);
        account.setAcctActiveStatus("Y");
        account.setAcctCurrBal(new BigDecimal("1000.00"));
        account.setAcctCreditLimit(new BigDecimal("5000.00"));
        account.setAcctExpirationDate(LocalDate.now().plusYears(1));
    }

    @Nested
    @DisplayName("Account Status Tests")
    class AccountStatusTests {

        @Test
        @DisplayName("isActive returns true for status Y")
        void isActive_StatusY_ReturnsTrue() {
            account.setAcctActiveStatus("Y");
            assertTrue(account.isActive());
        }

        @Test
        @DisplayName("isActive returns false for status N")
        void isActive_StatusN_ReturnsFalse() {
            account.setAcctActiveStatus("N");
            assertFalse(account.isActive());
        }

        @Test
        @DisplayName("isActive returns false for null status")
        void isActive_NullStatus_ReturnsFalse() {
            account.setAcctActiveStatus(null);
            assertFalse(account.isActive());
        }

        @Test
        @DisplayName("isExpired returns false for future expiration date")
        void isExpired_FutureDate_ReturnsFalse() {
            account.setAcctExpirationDate(LocalDate.now().plusDays(1));
            assertFalse(account.isExpired());
        }

        @Test
        @DisplayName("isExpired returns true for past expiration date")
        void isExpired_PastDate_ReturnsTrue() {
            account.setAcctExpirationDate(LocalDate.now().minusDays(1));
            assertTrue(account.isExpired());
        }

        @Test
        @DisplayName("isExpired returns false for today's date")
        void isExpired_TodayDate_ReturnsFalse() {
            account.setAcctExpirationDate(LocalDate.now());
            assertFalse(account.isExpired());
        }

        @Test
        @DisplayName("isExpired returns false for null expiration date")
        void isExpired_NullDate_ReturnsFalse() {
            account.setAcctExpirationDate(null);
            assertFalse(account.isExpired());
        }
    }

    @Nested
    @DisplayName("Credit Calculation Tests")
    class CreditCalculationTests {

        @Test
        @DisplayName("getAvailableCredit returns correct value")
        void getAvailableCredit_ReturnsCorrectValue() {
            account.setAcctCurrBal(new BigDecimal("1000.00"));
            account.setAcctCreditLimit(new BigDecimal("5000.00"));

            BigDecimal availableCredit = account.getAvailableCredit();

            assertEquals(new BigDecimal("4000.00"), availableCredit);
        }

        @Test
        @DisplayName("getAvailableCredit with zero balance returns full limit")
        void getAvailableCredit_ZeroBalance_ReturnsFullLimit() {
            account.setAcctCurrBal(BigDecimal.ZERO);
            account.setAcctCreditLimit(new BigDecimal("5000.00"));

            BigDecimal availableCredit = account.getAvailableCredit();

            assertEquals(new BigDecimal("5000.00"), availableCredit);
        }

        @Test
        @DisplayName("getAvailableCredit with overlimit balance returns negative")
        void getAvailableCredit_OverlimitBalance_ReturnsNegative() {
            account.setAcctCurrBal(new BigDecimal("6000.00"));
            account.setAcctCreditLimit(new BigDecimal("5000.00"));

            BigDecimal availableCredit = account.getAvailableCredit();

            assertEquals(new BigDecimal("-1000.00"), availableCredit);
        }

        @Test
        @DisplayName("getProjectedBalance calculates correctly for debit")
        void getProjectedBalance_Debit_CalculatesCorrectly() {
            account.setAcctCurrBal(new BigDecimal("1000.00"));

            BigDecimal projectedBalance = account.getProjectedBalance(new BigDecimal("500.00"));

            assertEquals(new BigDecimal("1500.00"), projectedBalance);
        }

        @Test
        @DisplayName("getProjectedBalance calculates correctly for credit")
        void getProjectedBalance_Credit_CalculatesCorrectly() {
            account.setAcctCurrBal(new BigDecimal("1000.00"));

            BigDecimal projectedBalance = account.getProjectedBalance(new BigDecimal("-500.00"));

            assertEquals(new BigDecimal("500.00"), projectedBalance);
        }
    }
}
