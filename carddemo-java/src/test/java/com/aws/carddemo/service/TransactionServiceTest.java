package com.aws.carddemo.service;

import com.aws.carddemo.dto.TransactionDto;
import com.aws.carddemo.entity.Account;
import com.aws.carddemo.entity.Card;
import com.aws.carddemo.entity.CardXref;
import com.aws.carddemo.entity.Customer;
import com.aws.carddemo.exception.TransactionValidationException;
import com.aws.carddemo.mapper.TransactionMapper;
import com.aws.carddemo.repository.AccountRepository;
import com.aws.carddemo.repository.CardRepository;
import com.aws.carddemo.repository.CardXrefRepository;
import com.aws.carddemo.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardXrefRepository cardXrefRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionService transactionService;

    private Account validAccount;
    private Card validCard;
    private CardXref validCardXref;
    private Customer validCustomer;

    @BeforeEach
    void setUp() {
        validAccount = new Account();
        validAccount.setAcctId(12345678901L);
        validAccount.setAcctActiveStatus("Y");
        validAccount.setAcctCurrBal(new BigDecimal("1000.00"));
        validAccount.setAcctCreditLimit(new BigDecimal("5000.00"));
        validAccount.setAcctExpirationDate(LocalDate.now().plusYears(1));
        validAccount.setAcctCurrCycCredit(BigDecimal.ZERO);
        validAccount.setAcctCurrCycDebit(BigDecimal.ZERO);

        validCard = new Card();
        validCard.setCardNum("4111111111111111");
        validCard.setCardActiveStatus("Y");
        validCard.setAccount(validAccount);

        validCustomer = new Customer();
        validCustomer.setCustId(123456789L);

        validCardXref = new CardXref();
        validCardXref.setCardNum("4111111111111111");
        validCardXref.setCard(validCard);
        validCardXref.setAccount(validAccount);
        validCardXref.setCustomer(validCustomer);
    }

    @Nested
    @DisplayName("Transaction Validation Tests - CBTRN02C Business Rules")
    class TransactionValidationTests {

        @Test
        @DisplayName("Validation Rule 100: INVALID CARD NUMBER FOUND - Card not in XREF")
        void validateTransaction_InvalidCardNumber_ThrowsException() {
            TransactionDto dto = createTransactionDto("9999999999999999", new BigDecimal("100.00"));
            
            when(cardXrefRepository.findByCardNumWithDetails("9999999999999999"))
                    .thenReturn(Optional.empty());

            TransactionValidationException exception = assertThrows(
                    TransactionValidationException.class,
                    () -> transactionService.validateTransaction(dto)
            );

            assertEquals(100, exception.getErrorCode());
            assertEquals("INVALID CARD NUMBER FOUND", exception.getMessage());
        }

        @Test
        @DisplayName("Validation Rule 101: ACCOUNT RECORD NOT FOUND")
        void validateTransaction_AccountNotFound_ThrowsException() {
            TransactionDto dto = createTransactionDto("4111111111111111", new BigDecimal("100.00"));
            
            CardXref xrefWithoutAccount = new CardXref();
            xrefWithoutAccount.setCardNum("4111111111111111");
            xrefWithoutAccount.setCard(validCard);
            xrefWithoutAccount.setAccount(null);
            
            when(cardXrefRepository.findByCardNumWithDetails("4111111111111111"))
                    .thenReturn(Optional.of(xrefWithoutAccount));

            TransactionValidationException exception = assertThrows(
                    TransactionValidationException.class,
                    () -> transactionService.validateTransaction(dto)
            );

            assertEquals(101, exception.getErrorCode());
            assertEquals("ACCOUNT RECORD NOT FOUND", exception.getMessage());
        }

        @Test
        @DisplayName("Validation Rule 102: OVERLIMIT TRANSACTION - Projected balance exceeds credit limit")
        void validateTransaction_OverlimitTransaction_ThrowsException() {
            TransactionDto dto = createTransactionDto("4111111111111111", new BigDecimal("5000.00"));
            
            when(cardXrefRepository.findByCardNumWithDetails("4111111111111111"))
                    .thenReturn(Optional.of(validCardXref));

            TransactionValidationException exception = assertThrows(
                    TransactionValidationException.class,
                    () -> transactionService.validateTransaction(dto)
            );

            assertEquals(102, exception.getErrorCode());
            assertEquals("OVERLIMIT TRANSACTION", exception.getMessage());
        }

        @Test
        @DisplayName("Validation Rule 102: OVERLIMIT TRANSACTION - Exactly at limit should pass")
        void validateTransaction_ExactlyAtLimit_ShouldPass() {
            TransactionDto dto = createTransactionDto("4111111111111111", new BigDecimal("4000.00"));
            
            when(cardXrefRepository.findByCardNumWithDetails("4111111111111111"))
                    .thenReturn(Optional.of(validCardXref));

            assertDoesNotThrow(() -> transactionService.validateTransaction(dto));
        }

        @Test
        @DisplayName("Validation Rule 103: TRANSACTION RECEIVED AFTER ACCT EXPIRATION")
        void validateTransaction_AccountExpired_ThrowsException() {
            validAccount.setAcctExpirationDate(LocalDate.now().minusDays(1));
            
            TransactionDto dto = createTransactionDto("4111111111111111", new BigDecimal("100.00"));
            dto.setTranOrigTs(LocalDateTime.now());
            
            when(cardXrefRepository.findByCardNumWithDetails("4111111111111111"))
                    .thenReturn(Optional.of(validCardXref));

            TransactionValidationException exception = assertThrows(
                    TransactionValidationException.class,
                    () -> transactionService.validateTransaction(dto)
            );

            assertEquals(103, exception.getErrorCode());
            assertEquals("TRANSACTION RECEIVED AFTER ACCT EXPIRATION", exception.getMessage());
        }

        @Test
        @DisplayName("Valid transaction should pass all validation rules")
        void validateTransaction_ValidTransaction_ShouldPass() {
            TransactionDto dto = createTransactionDto("4111111111111111", new BigDecimal("100.00"));
            
            when(cardXrefRepository.findByCardNumWithDetails("4111111111111111"))
                    .thenReturn(Optional.of(validCardXref));

            assertDoesNotThrow(() -> transactionService.validateTransaction(dto));
        }

        @Test
        @DisplayName("Credit transaction should pass validation")
        void validateTransaction_CreditTransaction_ShouldPass() {
            TransactionDto dto = createTransactionDto("4111111111111111", new BigDecimal("-500.00"));
            
            when(cardXrefRepository.findByCardNumWithDetails("4111111111111111"))
                    .thenReturn(Optional.of(validCardXref));

            assertDoesNotThrow(() -> transactionService.validateTransaction(dto));
        }

        @Test
        @DisplayName("Transaction on expiration date should pass")
        void validateTransaction_OnExpirationDate_ShouldPass() {
            validAccount.setAcctExpirationDate(LocalDate.now());
            
            TransactionDto dto = createTransactionDto("4111111111111111", new BigDecimal("100.00"));
            dto.setTranOrigTs(LocalDateTime.now().minusHours(1));
            
            when(cardXrefRepository.findByCardNumWithDetails("4111111111111111"))
                    .thenReturn(Optional.of(validCardXref));

            assertDoesNotThrow(() -> transactionService.validateTransaction(dto));
        }
    }

    @Nested
    @DisplayName("Transaction Posting Tests")
    class TransactionPostingTests {

        @Test
        @DisplayName("Post valid transaction updates account balance")
        void postTransaction_ValidTransaction_UpdatesAccountBalance() {
            TransactionDto dto = createTransactionDto("4111111111111111", new BigDecimal("100.00"));
            
            when(cardXrefRepository.findByCardNumWithDetails("4111111111111111"))
                    .thenReturn(Optional.of(validCardXref));
            when(transactionMapper.toEntity(any())).thenReturn(new com.aws.carddemo.entity.Transaction());
            when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(transactionMapper.toDto(any())).thenReturn(dto);

            transactionService.postTransaction(dto);

            verify(accountRepository).save(any(Account.class));
        }
    }

    private TransactionDto createTransactionDto(String cardNum, BigDecimal amount) {
        return TransactionDto.builder()
                .tranId("TRN" + System.currentTimeMillis())
                .tranCardNum(cardNum)
                .tranAmt(amount)
                .tranTypeCd("PU")
                .tranCatCd(1)
                .tranOrigTs(LocalDateTime.now())
                .build();
    }
}
