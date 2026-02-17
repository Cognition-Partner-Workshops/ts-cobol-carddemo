package com.aws.carddemo.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.aws.carddemo.account.Account;
import com.aws.carddemo.account.AccountRepository;
import com.aws.carddemo.billing.CategoryBalance;
import com.aws.carddemo.billing.CategoryBalanceId;
import com.aws.carddemo.billing.CategoryBalanceRepository;
import com.aws.carddemo.card.Card;
import com.aws.carddemo.card.CardRepository;
import com.aws.carddemo.card.CardXref;
import com.aws.carddemo.card.CardXrefRepository;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.exception.ValidationException;
import com.aws.carddemo.transaction.dto.TransactionCreateRequest;
import com.aws.carddemo.transaction.dto.TransactionListItemResponse;
import com.aws.carddemo.transaction.dto.TransactionResponse;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTests {

    @Mock
    private TransactionRecordRepository transactionRecordRepository;

    @Mock
    private TransactionTypeRepository transactionTypeRepository;

    @Mock
    private TransactionCategoryRepository transactionCategoryRepository;

    @Mock
    private CardXrefRepository cardXrefRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CategoryBalanceRepository categoryBalanceRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Account testAccount;
    private Card testCard;
    private CardXref testCardXref;
    private TransactionType testType;
    private TransactionCategory testCategory;
    private TransactionRecord testRecord;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setAccountStatus("A");
        testAccount.setCreditLimit(new BigDecimal("10000.00"));
        testAccount.setCurrentBalance(new BigDecimal("2500.00"));
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

        testCardXref = new CardXref();
        testCardXref.setCardNumber("4111111111111111");
        testCardXref.setAccount(testAccount);

        testType = new TransactionType();
        testType.setTypeCd("SA");
        testType.setTypeDesc("Sale");

        testCategory = new TransactionCategory();
        testCategory.setCatCd("0001");
        testCategory.setCatDesc("Retail Purchase");
        testCategory.setTransactionType(testType);

        testRecord = new TransactionRecord();
        testRecord.setId(1L);
        testRecord.setCard(testCard);
        testRecord.setTransactionType("SA");
        testRecord.setTransactionCategory("0001");
        testRecord.setTransactionSource("POS");
        testRecord.setDescription("Test purchase");
        testRecord.setAmount(new BigDecimal("50.00"));
        testRecord.setTimestamp(LocalDateTime.of(2025, 6, 15, 10, 30));
        testRecord.setMerchantId("MERCH001");
        testRecord.setMerchantName("Test Store");
        testRecord.setMerchantCity("Springfield");
        testRecord.setMerchantZip("62701");
    }

    @Test
    @SuppressWarnings("unchecked")
    void listTransactions_returnsPagedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TransactionRecord> page = new PageImpl<>(List.of(testRecord), pageable, 1);
        when(transactionRecordRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<TransactionListItemResponse> result = transactionService.listTransactions(
                "4111111111111111", null, null, null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        TransactionListItemResponse item = result.getContent().get(0);
        assertThat(item.maskedCardNumber()).isEqualTo("************1111");
        assertThat(item.amount()).isEqualTo(new BigDecimal("50.00"));
        assertThat(item.typeCode()).isEqualTo("SA");
    }

    @Test
    @SuppressWarnings("unchecked")
    void listTransactions_emptyResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TransactionRecord> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(transactionRecordRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        Page<TransactionListItemResponse> result = transactionService.listTransactions(
                "9999999999999999", null, null, null, null, null, pageable);

        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void getTransaction_returnsDetails() {
        when(transactionRecordRepository.findById(1L)).thenReturn(Optional.of(testRecord));
        when(transactionTypeRepository.findById("SA")).thenReturn(Optional.of(testType));
        when(transactionCategoryRepository.findById("0001")).thenReturn(Optional.of(testCategory));

        TransactionResponse result = transactionService.getTransaction(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.cardNumber()).isEqualTo("4111111111111111");
        assertThat(result.typeDescription()).isEqualTo("Sale");
        assertThat(result.categoryDescription()).isEqualTo("Retail Purchase");
        assertThat(result.amount()).isEqualTo(new BigDecimal("50.00"));
    }

    @Test
    void getTransaction_notFound_throwsException() {
        when(transactionRecordRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransaction(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found");
    }

    @Test
    void createTransaction_success() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                "4111111111111111", "SA", "0001", "POS", "Test purchase",
                new BigDecimal("75.00"), null, null,
                "MERCH001", "Test Store", "Springfield", "62701", true);

        when(cardXrefRepository.findById("4111111111111111")).thenReturn(Optional.of(testCardXref));
        when(transactionTypeRepository.findById("SA")).thenReturn(Optional.of(testType));
        when(transactionCategoryRepository.findById("0001")).thenReturn(Optional.of(testCategory));
        when(cardRepository.findByCardNumber("4111111111111111")).thenReturn(Optional.of(testCard));
        when(categoryBalanceRepository.findById(any(CategoryBalanceId.class))).thenReturn(Optional.empty());
        when(transactionRecordRepository.save(any(TransactionRecord.class))).thenAnswer(invocation -> {
            TransactionRecord saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });
        when(categoryBalanceRepository.save(any(CategoryBalance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse result = transactionService.createTransaction(request);

        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.amount()).isEqualTo(new BigDecimal("75.00"));
        assertThat(result.typeDescription()).isEqualTo("Sale");
        assertThat(result.categoryDescription()).isEqualTo("Retail Purchase");

        verify(transactionRecordRepository).save(any(TransactionRecord.class));
        verify(categoryBalanceRepository).save(any(CategoryBalance.class));
    }

    @Test
    void createTransaction_notConfirmed_throwsValidation() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                "4111111111111111", "SA", "0001", "POS", "Test",
                new BigDecimal("50.00"), null, null,
                null, null, null, null, false);

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("confirmed");

        verify(transactionRecordRepository, never()).save(any());
    }

    @Test
    void createTransaction_invalidCard_throwsValidation() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                "9999999999999999", "SA", "0001", "POS", "Test",
                new BigDecimal("50.00"), null, null,
                null, null, null, null, true);

        when(cardXrefRepository.findById("9999999999999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Card number not found");

        verify(transactionRecordRepository, never()).save(any());
    }

    @Test
    void createTransaction_inactiveAccount_throwsValidation() {
        testAccount.setAccountStatus("C");
        TransactionCreateRequest request = new TransactionCreateRequest(
                "4111111111111111", "SA", "0001", "POS", "Test",
                new BigDecimal("50.00"), null, null,
                null, null, null, null, true);

        when(cardXrefRepository.findById("4111111111111111")).thenReturn(Optional.of(testCardXref));

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not active");

        verify(transactionRecordRepository, never()).save(any());
    }

    @Test
    void createTransaction_invalidTypeCode_throwsValidation() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                "4111111111111111", "XX", "0001", "POS", "Test",
                new BigDecimal("50.00"), null, null,
                null, null, null, null, true);

        when(cardXrefRepository.findById("4111111111111111")).thenReturn(Optional.of(testCardXref));
        when(transactionTypeRepository.findById("XX")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid transaction type");

        verify(transactionRecordRepository, never()).save(any());
    }

    @Test
    void createTransaction_invalidCategoryCode_throwsValidation() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                "4111111111111111", "SA", "XXXX", "POS", "Test",
                new BigDecimal("50.00"), null, null,
                null, null, null, null, true);

        when(cardXrefRepository.findById("4111111111111111")).thenReturn(Optional.of(testCardXref));
        when(transactionTypeRepository.findById("SA")).thenReturn(Optional.of(testType));
        when(transactionCategoryRepository.findById("XXXX")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid transaction category");

        verify(transactionRecordRepository, never()).save(any());
    }

    @Test
    void createTransaction_updatesCategoryBalance_existingBalance() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                "4111111111111111", "SA", "0001", "POS", "Test",
                new BigDecimal("100.00"), null, null,
                null, null, null, null, true);

        CategoryBalance existingBalance = new CategoryBalance();
        existingBalance.setAccount(testAccount);
        existingBalance.setCategory(testCategory);
        existingBalance.setBalance(new BigDecimal("500.00"));

        when(cardXrefRepository.findById("4111111111111111")).thenReturn(Optional.of(testCardXref));
        when(transactionTypeRepository.findById("SA")).thenReturn(Optional.of(testType));
        when(transactionCategoryRepository.findById("0001")).thenReturn(Optional.of(testCategory));
        when(cardRepository.findByCardNumber("4111111111111111")).thenReturn(Optional.of(testCard));
        when(categoryBalanceRepository.findById(any(CategoryBalanceId.class))).thenReturn(Optional.of(existingBalance));
        when(transactionRecordRepository.save(any(TransactionRecord.class))).thenAnswer(invocation -> {
            TransactionRecord saved = invocation.getArgument(0);
            saved.setId(101L);
            return saved;
        });
        when(categoryBalanceRepository.save(any(CategoryBalance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        transactionService.createTransaction(request);

        ArgumentCaptor<CategoryBalance> captor = ArgumentCaptor.forClass(CategoryBalance.class);
        verify(categoryBalanceRepository).save(captor.capture());
        assertThat(captor.getValue().getBalance()).isEqualByComparingTo(new BigDecimal("600.00"));
    }

    @Test
    void createTransaction_createsCategoryBalance_newBalance() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                "4111111111111111", "SA", "0001", "POS", "Test",
                new BigDecimal("200.00"), null, null,
                null, null, null, null, true);

        when(cardXrefRepository.findById("4111111111111111")).thenReturn(Optional.of(testCardXref));
        when(transactionTypeRepository.findById("SA")).thenReturn(Optional.of(testType));
        when(transactionCategoryRepository.findById("0001")).thenReturn(Optional.of(testCategory));
        when(cardRepository.findByCardNumber("4111111111111111")).thenReturn(Optional.of(testCard));
        when(categoryBalanceRepository.findById(any(CategoryBalanceId.class))).thenReturn(Optional.empty());
        when(transactionRecordRepository.save(any(TransactionRecord.class))).thenAnswer(invocation -> {
            TransactionRecord saved = invocation.getArgument(0);
            saved.setId(102L);
            return saved;
        });
        when(categoryBalanceRepository.save(any(CategoryBalance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        transactionService.createTransaction(request);

        ArgumentCaptor<CategoryBalance> captor = ArgumentCaptor.forClass(CategoryBalance.class);
        verify(categoryBalanceRepository).save(captor.capture());
        assertThat(captor.getValue().getBalance()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    void createTransaction_setsProcessingTimestamp() {
        LocalDateTime processingDate = LocalDateTime.of(2025, 8, 1, 14, 0);
        TransactionCreateRequest request = new TransactionCreateRequest(
                "4111111111111111", "SA", "0001", "POS", "Test",
                new BigDecimal("50.00"), null, processingDate,
                null, null, null, null, true);

        when(cardXrefRepository.findById("4111111111111111")).thenReturn(Optional.of(testCardXref));
        when(transactionTypeRepository.findById("SA")).thenReturn(Optional.of(testType));
        when(transactionCategoryRepository.findById("0001")).thenReturn(Optional.of(testCategory));
        when(cardRepository.findByCardNumber("4111111111111111")).thenReturn(Optional.of(testCard));
        when(categoryBalanceRepository.findById(any(CategoryBalanceId.class))).thenReturn(Optional.empty());
        when(transactionRecordRepository.save(any(TransactionRecord.class))).thenAnswer(invocation -> {
            TransactionRecord saved = invocation.getArgument(0);
            saved.setId(103L);
            return saved;
        });
        when(categoryBalanceRepository.save(any(CategoryBalance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        transactionService.createTransaction(request);

        ArgumentCaptor<TransactionRecord> captor = ArgumentCaptor.forClass(TransactionRecord.class);
        verify(transactionRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getTimestamp()).isEqualTo(processingDate);
    }
}
