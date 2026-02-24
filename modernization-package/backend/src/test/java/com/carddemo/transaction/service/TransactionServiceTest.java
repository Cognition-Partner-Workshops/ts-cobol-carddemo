package com.carddemo.transaction.service;

import com.carddemo.transaction.dto.*;
import com.carddemo.transaction.entity.CardCrossReference;
import com.carddemo.transaction.entity.Transaction;
import com.carddemo.transaction.exception.DuplicateTransactionException;
import com.carddemo.transaction.exception.ResourceNotFoundException;
import com.carddemo.transaction.exception.ValidationException;
import com.carddemo.transaction.repository.CardCrossReferenceRepository;
import com.carddemo.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for TransactionService.
 * Covers all 30 Business Rules with exact error message parity from BRE Section 8.2.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CardCrossReferenceRepository xrefRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Transaction sampleTransaction;
    private CardCrossReference sampleXref;

    @BeforeEach
    void setUp() {
        sampleTransaction = new Transaction();
        sampleTransaction.setTransactionId("0000000000000001");
        sampleTransaction.setCardNumber("4111111111111111");
        sampleTransaction.setTypeCode("01");
        sampleTransaction.setCategoryCode(new BigDecimal("5001"));
        sampleTransaction.setSource("ONLINE");
        sampleTransaction.setDescription("Monthly Subscription Service");
        sampleTransaction.setAmount(new BigDecimal("-14.99"));
        sampleTransaction.setMerchantId(new BigDecimal("100000001"));
        sampleTransaction.setMerchantName("StreamFlix");
        sampleTransaction.setMerchantCity("Los Angeles");
        sampleTransaction.setMerchantZip("90001");
        sampleTransaction.setOriginationTs(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        sampleTransaction.setProcessingTs(LocalDateTime.of(2024, 1, 1, 10, 0, 5));

        sampleXref = new CardCrossReference();
        sampleXref.setCardNumber("4111111111111111");
        sampleXref.setAccountId(new BigDecimal("1"));
        sampleXref.setCustomerId(new BigDecimal("100000001"));
    }

    /**
     * Helper to build a valid AddTransactionRequest for testing.
     * Caller can override specific fields to test individual validations.
     */
    private AddTransactionRequest buildValidRequest() {
        AddTransactionRequest request = new AddTransactionRequest();
        request.setAccountId("1");
        request.setCardNumber(null);
        request.setTypeCode("01");
        request.setCategoryCode("5001");
        request.setSource("ONLINE");
        request.setDescription("Test Transaction");
        request.setAmount("100.00");
        request.setOriginationDate("2024-01-15");
        request.setProcessingDate("2024-01-15");
        request.setMerchantId("100000001");
        request.setMerchantName("TestMerchant");
        request.setMerchantCity("TestCity");
        request.setMerchantZip("10001");
        request.setConfirmation("Y");
        return request;
    }

    // ========================================================================
    // CT00 - List Transactions (BR-LT-01 through BR-LT-08)
    // ========================================================================

    @Nested
    @DisplayName("CT00 - List Transactions")
    class ListTransactionsTests {

        @Test
        @DisplayName("BR-LT-01: Page size defaults to 10 when size <= 0")
        void testListTransactions_DefaultPageSize() {
            Page<Transaction> emptyPage = new PageImpl<>(
                    Collections.emptyList(), PageRequest.of(0, 10), 0);
            when(transactionRepository.findAllByOrderByTransactionIdAsc(any(Pageable.class)))
                    .thenReturn(emptyPage);

            TransactionListResponse response = transactionService.listTransactions(0, -1, null);

            assertNotNull(response);
            assertEquals(10, response.getSize());
        }

        @Test
        @DisplayName("BR-LT-01: Page size of 10 returns 10 records per page")
        void testListTransactions_PageSizeOf10() {
            List<Transaction> transactions = Collections.nCopies(10, sampleTransaction);
            Page<Transaction> page = new PageImpl<>(transactions, PageRequest.of(0, 10), 15);
            when(transactionRepository.findAllByOrderByTransactionIdAsc(any(Pageable.class)))
                    .thenReturn(page);

            TransactionListResponse response = transactionService.listTransactions(0, 10, null);

            assertEquals(10, response.getContent().size());
            assertEquals(10, response.getSize());
        }

        @Test
        @DisplayName("BR-LT-02: Non-numeric filter throws IllegalArgumentException with exact message")
        void testListTransactions_NonNumericFilter_ThrowsException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> transactionService.listTransactions(0, 10, "ABC"));

            assertEquals("Tran ID must be Numeric ...", ex.getMessage());
        }

        @Test
        @DisplayName("BR-LT-02: Non-numeric filter with special characters")
        void testListTransactions_SpecialCharFilter_ThrowsException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> transactionService.listTransactions(0, 10, "12-34"));

            assertEquals("Tran ID must be Numeric ...", ex.getMessage());
        }

        @Test
        @DisplayName("BR-LT-04: Empty filter browses from start")
        void testListTransactions_EmptyFilter_BrowsesFromStart() {
            Page<Transaction> page = new PageImpl<>(
                    List.of(sampleTransaction), PageRequest.of(0, 10), 1);
            when(transactionRepository.findAllByOrderByTransactionIdAsc(any(Pageable.class)))
                    .thenReturn(page);

            TransactionListResponse response = transactionService.listTransactions(0, 10, null);

            verify(transactionRepository).findAllByOrderByTransactionIdAsc(any(Pageable.class));
            verify(transactionRepository, never())
                    .findByTransactionIdGreaterThanEqualOrderByTransactionIdAsc(anyString(), any());
        }

        @Test
        @DisplayName("BR-LT-04: Blank filter browses from start")
        void testListTransactions_BlankFilter_BrowsesFromStart() {
            Page<Transaction> page = new PageImpl<>(
                    List.of(sampleTransaction), PageRequest.of(0, 10), 1);
            when(transactionRepository.findAllByOrderByTransactionIdAsc(any(Pageable.class)))
                    .thenReturn(page);

            TransactionListResponse response = transactionService.listTransactions(0, 10, "   ");

            verify(transactionRepository).findAllByOrderByTransactionIdAsc(any(Pageable.class));
        }

        @Test
        @DisplayName("BR-LT-04: Numeric filter starts from specified ID")
        void testListTransactions_NumericFilter_StartsFromId() {
            Page<Transaction> page = new PageImpl<>(
                    List.of(sampleTransaction), PageRequest.of(0, 10), 1);
            when(transactionRepository
                    .findByTransactionIdGreaterThanEqualOrderByTransactionIdAsc(anyString(), any(Pageable.class)))
                    .thenReturn(page);

            TransactionListResponse response = transactionService.listTransactions(0, 10, "5");

            verify(transactionRepository)
                    .findByTransactionIdGreaterThanEqualOrderByTransactionIdAsc(eq("0000000000000005"), any());
        }

        @Test
        @DisplayName("BR-LT-05: Last page sets isLast=true and hasNext=false (forward boundary)")
        void testListTransactions_LastPage_IsLastTrue() {
            List<Transaction> lastPageContent = List.of(sampleTransaction);
            Page<Transaction> lastPage = new PageImpl<>(lastPageContent, PageRequest.of(1, 10), 11);
            when(transactionRepository.findAllByOrderByTransactionIdAsc(any(Pageable.class)))
                    .thenReturn(lastPage);

            TransactionListResponse response = transactionService.listTransactions(1, 10, null);

            assertTrue(response.isLast());
            assertFalse(response.isHasNext());
        }

        @Test
        @DisplayName("BR-LT-06: First page sets isFirst=true and hasPrevious=false (backward boundary)")
        void testListTransactions_FirstPage_IsFirstTrue() {
            Page<Transaction> firstPage = new PageImpl<>(
                    List.of(sampleTransaction), PageRequest.of(0, 10), 15);
            when(transactionRepository.findAllByOrderByTransactionIdAsc(any(Pageable.class)))
                    .thenReturn(firstPage);

            TransactionListResponse response = transactionService.listTransactions(0, 10, null);

            assertTrue(response.isFirst());
            assertFalse(response.isHasPrevious());
        }

        @Test
        @DisplayName("BR-LT-07: Page state is preserved in response (page number, size, totals)")
        void testListTransactions_PageStatePreserved() {
            List<Transaction> content = Collections.nCopies(10, sampleTransaction);
            Page<Transaction> page = new PageImpl<>(content, PageRequest.of(0, 10), 15);
            when(transactionRepository.findAllByOrderByTransactionIdAsc(any(Pageable.class)))
                    .thenReturn(page);

            TransactionListResponse response = transactionService.listTransactions(0, 10, null);

            assertEquals(0, response.getPage());
            assertEquals(10, response.getSize());
            assertEquals(15, response.getTotalElements());
            assertEquals(2, response.getTotalPages());
            assertTrue(response.isHasNext());
        }

        @Test
        @DisplayName("BR-LT-05/LT-06: Empty result set")
        void testListTransactions_EmptyResultSet() {
            Page<Transaction> emptyPage = new PageImpl<>(
                    Collections.emptyList(), PageRequest.of(0, 10), 0);
            when(transactionRepository.findAllByOrderByTransactionIdAsc(any(Pageable.class)))
                    .thenReturn(emptyPage);

            TransactionListResponse response = transactionService.listTransactions(0, 10, null);

            assertTrue(response.getContent().isEmpty());
            assertEquals(0, response.getTotalElements());
            assertTrue(response.isFirst());
            assertTrue(response.isLast());
        }
    }

    // ========================================================================
    // CT01 - View Transaction (BR-VT-01 through BR-VT-05)
    // ========================================================================

    @Nested
    @DisplayName("CT01 - View Transaction")
    class ViewTransactionTests {

        @Test
        @DisplayName("BR-VT-01: Empty transaction ID throws ResourceNotFoundException with exact message")
        void testViewTransaction_EmptyId_ThrowsResourceNotFound() {
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> transactionService.viewTransaction(""));

            assertEquals("Tran ID can NOT be empty...", ex.getMessage());
            assertEquals("transactionId", ex.getField());
            assertEquals("BR-VT-01", ex.getBusinessRule());
        }

        @Test
        @DisplayName("BR-VT-01: Null transaction ID throws ResourceNotFoundException")
        void testViewTransaction_NullId_ThrowsResourceNotFound() {
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> transactionService.viewTransaction(null));

            assertEquals("Tran ID can NOT be empty...", ex.getMessage());
        }

        @Test
        @DisplayName("BR-VT-01: Blank transaction ID throws ResourceNotFoundException")
        void testViewTransaction_BlankId_ThrowsResourceNotFound() {
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> transactionService.viewTransaction("   "));

            assertEquals("Tran ID can NOT be empty...", ex.getMessage());
        }

        @Test
        @DisplayName("BR-VT-01/VT-02: Non-existent transaction ID throws ResourceNotFoundException with exact message")
        void testViewTransaction_NotFound_ThrowsResourceNotFound() {
            when(transactionRepository.findById("9999999999999999")).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> transactionService.viewTransaction("9999999999999999"));

            assertEquals("Transaction ID NOT found...", ex.getMessage());
            assertEquals("transactionId", ex.getField());
            assertEquals("BR-VT-01", ex.getBusinessRule());
        }

        @Test
        @DisplayName("BR-VT-02/VT-03: Existing transaction returns all 13 fields")
        void testViewTransaction_ExistingId_ReturnsAllFields() {
            when(transactionRepository.findById("0000000000000001"))
                    .thenReturn(Optional.of(sampleTransaction));
            when(xrefRepository.findByCardNumber("4111111111111111"))
                    .thenReturn(Optional.of(sampleXref));

            TransactionDetailResponse response = transactionService.viewTransaction("0000000000000001");

            assertNotNull(response);
            assertEquals("0000000000000001", response.getTransactionId());
            assertEquals("4111111111111111", response.getCardNumber());
            assertEquals("01", response.getTypeCode());
            assertEquals(5001, response.getCategoryCode());
            assertEquals("ONLINE", response.getSource());
            assertEquals("Monthly Subscription Service", response.getDescription());
            assertEquals(new BigDecimal("-14.99"), response.getAmount());
            assertEquals(100000001L, response.getMerchantId());
            assertEquals("StreamFlix", response.getMerchantName());
            assertEquals("Los Angeles", response.getMerchantCity());
            assertEquals("90001", response.getMerchantZip());
            assertNotNull(response.getOriginationTimestamp());
            assertNotNull(response.getProcessingTimestamp());
            // Account ID resolved from cross-reference
            assertEquals("00000000001", response.getAccountId());
        }

        @Test
        @DisplayName("BR-VT-03: Detail response includes resolved Account ID from cross-ref")
        void testViewTransaction_ResolvesAccountIdFromXref() {
            when(transactionRepository.findById("0000000000000001"))
                    .thenReturn(Optional.of(sampleTransaction));
            when(xrefRepository.findByCardNumber("4111111111111111"))
                    .thenReturn(Optional.of(sampleXref));

            TransactionDetailResponse response = transactionService.viewTransaction("0000000000000001");

            assertEquals("00000000001", response.getAccountId());
        }

        @Test
        @DisplayName("BR-VT-03: Detail response with no cross-ref leaves Account ID null")
        void testViewTransaction_NoXref_AccountIdNull() {
            when(transactionRepository.findById("0000000000000001"))
                    .thenReturn(Optional.of(sampleTransaction));
            when(xrefRepository.findByCardNumber("4111111111111111"))
                    .thenReturn(Optional.empty());

            TransactionDetailResponse response = transactionService.viewTransaction("0000000000000001");

            assertNull(response.getAccountId());
        }
    }

    // ========================================================================
    // CT02 - Add Transaction: 6-Phase Validation Chain (BR-AT-01 through BR-AT-14)
    // ========================================================================

    @Nested
    @DisplayName("CT02 - Phase 1: Key Field Validation (BR-AT-01 through BR-AT-05)")
    class Phase1Tests {

        @Test
        @DisplayName("BR-AT-01: No account and no card throws ValidationException")
        void testPhase1_NoKeyFields() {
            AddTransactionRequest request = buildValidRequest();
            request.setAccountId(null);
            request.setCardNumber(null);

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Account or Card Number must be entered...", ex.getMessage());
            assertEquals(1, ex.getPhase());
            assertEquals("accountId", ex.getField());
            assertEquals("BR-AT-01", ex.getBusinessRule());
            assertEquals(400, ex.getHttpStatus());
        }

        @Test
        @DisplayName("BR-AT-01: Both blank throws ValidationException")
        void testPhase1_BothBlank() {
            AddTransactionRequest request = buildValidRequest();
            request.setAccountId("");
            request.setCardNumber("  ");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Account or Card Number must be entered...", ex.getMessage());
        }

        @Test
        @DisplayName("BR-AT-02: Non-numeric account ID throws ValidationException")
        void testPhase1_NonNumericAccount() {
            AddTransactionRequest request = buildValidRequest();
            request.setAccountId("ABC");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Account ID must be Numeric...", ex.getMessage());
            assertEquals(1, ex.getPhase());
            assertEquals("accountId", ex.getField());
            assertEquals("BR-AT-02", ex.getBusinessRule());
        }

        @Test
        @DisplayName("BR-AT-02: Account ID with special characters")
        void testPhase1_AccountIdSpecialChars() {
            AddTransactionRequest request = buildValidRequest();
            request.setAccountId("123-456");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Account ID must be Numeric...", ex.getMessage());
        }

        @Test
        @DisplayName("BR-AT-03: Non-numeric card number throws ValidationException")
        void testPhase1_NonNumericCard() {
            AddTransactionRequest request = buildValidRequest();
            request.setAccountId(null);
            request.setCardNumber("XXXX1111YYYY2222");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Card Number must be Numeric...", ex.getMessage());
            assertEquals(1, ex.getPhase());
            assertEquals("cardNumber", ex.getField());
            assertEquals("BR-AT-03", ex.getBusinessRule());
        }

        @Test
        @DisplayName("BR-AT-04: Account not found in cross-reference")
        void testPhase1_AccountNotFound() {
            AddTransactionRequest request = buildValidRequest();
            request.setAccountId("99999");
            when(xrefRepository.findFirstByAccountId(new BigDecimal("99999")))
                    .thenReturn(Optional.empty());

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Account ID NOT found...", ex.getMessage());
            assertEquals(1, ex.getPhase());
            assertEquals("BR-AT-04", ex.getBusinessRule());
            assertEquals(404, ex.getHttpStatus());
        }

        @Test
        @DisplayName("BR-AT-04: Card number not found in cross-reference")
        void testPhase1_CardNotFound() {
            AddTransactionRequest request = buildValidRequest();
            request.setAccountId(null);
            request.setCardNumber("9999999999999999");
            when(xrefRepository.findByCardNumber("9999999999999999"))
                    .thenReturn(Optional.empty());

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Card Number NOT found...", ex.getMessage());
            assertEquals(1, ex.getPhase());
            assertEquals("BR-AT-04", ex.getBusinessRule());
            assertEquals(404, ex.getHttpStatus());
        }

        @Test
        @DisplayName("BR-AT-05: Account ID resolves to Card Number (Path A)")
        void testPhase1_AccountResolvesToCard() {
            AddTransactionRequest request = buildValidRequest();
            request.setAccountId("1");
            request.setCardNumber(null);
            request.setConfirmation(null); // Stop at confirmation gate
            when(xrefRepository.findFirstByAccountId(new BigDecimal("1")))
                    .thenReturn(Optional.of(sampleXref));

            Object result = transactionService.addTransaction(request);

            assertInstanceOf(ConfirmationRequiredResponse.class, result);
            ConfirmationRequiredResponse confirmResp = (ConfirmationRequiredResponse) result;
            assertEquals("00000000001", confirmResp.getResolvedAccountId());
            assertEquals("4111111111111111", confirmResp.getResolvedCardNumber());
        }

        @Test
        @DisplayName("BR-AT-05: Card Number resolves to Account ID (Path B)")
        void testPhase1_CardResolvesToAccount() {
            AddTransactionRequest request = buildValidRequest();
            request.setAccountId(null);
            request.setCardNumber("4111111111111111");
            request.setConfirmation(null); // Stop at confirmation gate
            when(xrefRepository.findByCardNumber("4111111111111111"))
                    .thenReturn(Optional.of(sampleXref));

            Object result = transactionService.addTransaction(request);

            assertInstanceOf(ConfirmationRequiredResponse.class, result);
            ConfirmationRequiredResponse confirmResp = (ConfirmationRequiredResponse) result;
            assertEquals("00000000001", confirmResp.getResolvedAccountId());
            assertEquals("4111111111111111", confirmResp.getResolvedCardNumber());
        }
    }

    @Nested
    @DisplayName("CT02 - Phase 2: Mandatory Field Checks (BR-AT-06)")
    class Phase2Tests {

        @BeforeEach
        void setUpXref() {
            lenient().when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));
        }

        @Test
        @DisplayName("BR-AT-06: Empty typeCode -> 'Type CD can NOT be empty...'")
        void testPhase2_EmptyTypeCode() {
            AddTransactionRequest request = buildValidRequest();
            request.setTypeCode(null);

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Type CD can NOT be empty...", ex.getMessage());
            assertEquals(2, ex.getPhase());
            assertEquals("typeCode", ex.getField());
            assertEquals("BR-AT-06", ex.getBusinessRule());
        }

        @Test
        @DisplayName("BR-AT-06: Blank typeCode -> 'Type CD can NOT be empty...'")
        void testPhase2_BlankTypeCode() {
            AddTransactionRequest request = buildValidRequest();
            request.setTypeCode("  ");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Type CD can NOT be empty...", ex.getMessage());
        }

        @Test
        @DisplayName("BR-AT-06: Empty categoryCode -> 'Category CD can NOT be empty...'")
        void testPhase2_EmptyCategoryCode() {
            AddTransactionRequest request = buildValidRequest();
            request.setCategoryCode("");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Category CD can NOT be empty...", ex.getMessage());
            assertEquals("categoryCode", ex.getField());
        }

        @Test
        @DisplayName("BR-AT-06: Empty source -> 'Source can NOT be empty...'")
        void testPhase2_EmptySource() {
            AddTransactionRequest request = buildValidRequest();
            request.setSource(null);

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Source can NOT be empty...", ex.getMessage());
            assertEquals("source", ex.getField());
        }

        @Test
        @DisplayName("BR-AT-06: Empty description -> 'Description can NOT be empty...'")
        void testPhase2_EmptyDescription() {
            AddTransactionRequest request = buildValidRequest();
            request.setDescription("");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Description can NOT be empty...", ex.getMessage());
            assertEquals("description", ex.getField());
        }

        @Test
        @DisplayName("BR-AT-06: Empty amount -> 'Amount can NOT be empty...'")
        void testPhase2_EmptyAmount() {
            AddTransactionRequest request = buildValidRequest();
            request.setAmount(null);

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Amount can NOT be empty...", ex.getMessage());
            assertEquals("amount", ex.getField());
        }

        @Test
        @DisplayName("BR-AT-06: Empty originationDate -> 'Orig Date can NOT be empty...'")
        void testPhase2_EmptyOrigDate() {
            AddTransactionRequest request = buildValidRequest();
            request.setOriginationDate("");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Orig Date can NOT be empty...", ex.getMessage());
            assertEquals("originationDate", ex.getField());
        }

        @Test
        @DisplayName("BR-AT-06: Empty processingDate -> 'Proc Date can NOT be empty...'")
        void testPhase2_EmptyProcDate() {
            AddTransactionRequest request = buildValidRequest();
            request.setProcessingDate(null);

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Proc Date can NOT be empty...", ex.getMessage());
            assertEquals("processingDate", ex.getField());
        }

        @Test
        @DisplayName("BR-AT-06: Empty merchantId -> 'Merchant ID can NOT be empty...'")
        void testPhase2_EmptyMerchantId() {
            AddTransactionRequest request = buildValidRequest();
            request.setMerchantId("");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Merchant ID can NOT be empty...", ex.getMessage());
            assertEquals("merchantId", ex.getField());
        }

        @Test
        @DisplayName("BR-AT-06: Empty merchantName -> 'Merchant Name can NOT be empty...'")
        void testPhase2_EmptyMerchantName() {
            AddTransactionRequest request = buildValidRequest();
            request.setMerchantName(null);

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Merchant Name can NOT be empty...", ex.getMessage());
            assertEquals("merchantName", ex.getField());
        }

        @Test
        @DisplayName("BR-AT-06: Empty merchantCity -> 'Merchant City can NOT be empty...'")
        void testPhase2_EmptyMerchantCity() {
            AddTransactionRequest request = buildValidRequest();
            request.setMerchantCity("");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Merchant City can NOT be empty...", ex.getMessage());
            assertEquals("merchantCity", ex.getField());
        }

        @Test
        @DisplayName("BR-AT-06: Empty merchantZip -> 'Merchant Zip can NOT be empty...'")
        void testPhase2_EmptyMerchantZip() {
            AddTransactionRequest request = buildValidRequest();
            request.setMerchantZip(null);

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Merchant Zip can NOT be empty...", ex.getMessage());
            assertEquals("merchantZip", ex.getField());
        }
    }

    @Nested
    @DisplayName("CT02 - Phase 3: Numeric Type Checks (BR-AT-07)")
    class Phase3Tests {

        @BeforeEach
        void setUpXref() {
            lenient().when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));
        }

        @Test
        @DisplayName("BR-AT-07: Non-numeric typeCode -> 'Type CD must be Numeric...'")
        void testPhase3_NonNumericTypeCode() {
            AddTransactionRequest request = buildValidRequest();
            request.setTypeCode("AB");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Type CD must be Numeric...", ex.getMessage());
            assertEquals(3, ex.getPhase());
            assertEquals("typeCode", ex.getField());
            assertEquals("BR-AT-07", ex.getBusinessRule());
        }

        @Test
        @DisplayName("BR-AT-07: Non-numeric categoryCode -> 'Category CD must be Numeric...'")
        void testPhase3_NonNumericCategoryCode() {
            AddTransactionRequest request = buildValidRequest();
            request.setCategoryCode("ABCD");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Category CD must be Numeric...", ex.getMessage());
            assertEquals(3, ex.getPhase());
            assertEquals("categoryCode", ex.getField());
            assertEquals("BR-AT-07", ex.getBusinessRule());
        }
    }

    @Nested
    @DisplayName("CT02 - Phase 4: Amount Format Validation (BR-AT-08)")
    class Phase4Tests {

        @BeforeEach
        void setUpXref() {
            lenient().when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));
        }

        @Test
        @DisplayName("BR-AT-08: Missing decimal -> exact error message")
        void testPhase4_MissingDecimal() {
            AddTransactionRequest request = buildValidRequest();
            request.setAmount("100");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Amount should be in format -99999999.99", ex.getMessage());
            assertEquals(4, ex.getPhase());
            assertEquals("amount", ex.getField());
            assertEquals("BR-AT-08", ex.getBusinessRule());
        }

        @Test
        @DisplayName("BR-AT-08: Too many decimal places")
        void testPhase4_TooManyDecimals() {
            AddTransactionRequest request = buildValidRequest();
            request.setAmount("100.123");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Amount should be in format -99999999.99", ex.getMessage());
        }

        @Test
        @DisplayName("BR-AT-08: Letters in amount")
        void testPhase4_LettersInAmount() {
            AddTransactionRequest request = buildValidRequest();
            request.setAmount("abc.00");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Amount should be in format -99999999.99", ex.getMessage());
        }

        @Test
        @DisplayName("BR-AT-08: Too many integer digits")
        void testPhase4_TooManyIntegerDigits() {
            AddTransactionRequest request = buildValidRequest();
            request.setAmount("123456789.00");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Amount should be in format -99999999.99", ex.getMessage());
        }

        @Test
        @DisplayName("BR-AT-08: Valid positive amount passes")
        void testPhase4_ValidPositiveAmount() {
            AddTransactionRequest request = buildValidRequest();
            request.setAmount("12345678.99");
            request.setConfirmation(null);
            when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));

            // Should not throw - reaches confirmation gate
            Object result = transactionService.addTransaction(request);
            assertInstanceOf(ConfirmationRequiredResponse.class, result);
        }

        @Test
        @DisplayName("BR-AT-08: Valid negative amount passes")
        void testPhase4_ValidNegativeAmount() {
            AddTransactionRequest request = buildValidRequest();
            request.setAmount("-12345678.99");
            request.setConfirmation(null);
            when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));

            Object result = transactionService.addTransaction(request);
            assertInstanceOf(ConfirmationRequiredResponse.class, result);
        }
    }

    @Nested
    @DisplayName("CT02 - Phase 5: Date Validation (BR-AT-09, BR-AT-10)")
    class Phase5Tests {

        @BeforeEach
        void setUpXref() {
            lenient().when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));
        }

        @Test
        @DisplayName("BR-AT-09: Bad origination date format -> exact message")
        void testPhase5_BadOrigDateFormat() {
            AddTransactionRequest request = buildValidRequest();
            request.setOriginationDate("01-01-2024");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Orig Date - Date format must be YYYY-MM-DD...", ex.getMessage());
            assertEquals(5, ex.getPhase());
            assertEquals("originationDate", ex.getField());
            assertEquals("BR-AT-09", ex.getBusinessRule());
        }

        @Test
        @DisplayName("BR-AT-09: Bad processing date format")
        void testPhase5_BadProcDateFormat() {
            AddTransactionRequest request = buildValidRequest();
            request.setProcessingDate("2024/01/01");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Proc Date - Date format must be YYYY-MM-DD...", ex.getMessage());
            assertEquals(5, ex.getPhase());
            assertEquals("processingDate", ex.getField());
        }

        @Test
        @DisplayName("BR-AT-10: Invalid origination date calendar (Feb 30)")
        void testPhase5_InvalidOrigCalendar() {
            AddTransactionRequest request = buildValidRequest();
            request.setOriginationDate("2024-02-30");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Orig Date - Not a valid date...", ex.getMessage());
            assertEquals(5, ex.getPhase());
            assertEquals("originationDate", ex.getField());
            assertEquals("BR-AT-10", ex.getBusinessRule());
        }

        @Test
        @DisplayName("BR-AT-10: Invalid processing date calendar (Month 13)")
        void testPhase5_InvalidProcCalendar() {
            AddTransactionRequest request = buildValidRequest();
            request.setProcessingDate("2024-13-01");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Proc Date - Not a valid date...", ex.getMessage());
            assertEquals(5, ex.getPhase());
            assertEquals("processingDate", ex.getField());
        }

        @Test
        @DisplayName("BR-AT-10: Feb 29 on non-leap year is invalid")
        void testPhase5_NonLeapYearFeb29() {
            AddTransactionRequest request = buildValidRequest();
            request.setOriginationDate("2023-02-29");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Orig Date - Not a valid date...", ex.getMessage());
        }

        @Test
        @DisplayName("BR-AT-09/10: Feb 29 on leap year is valid")
        void testPhase5_LeapYearFeb29Valid() {
            AddTransactionRequest request = buildValidRequest();
            request.setOriginationDate("2024-02-29");
            request.setConfirmation(null);
            when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));

            Object result = transactionService.addTransaction(request);
            assertInstanceOf(ConfirmationRequiredResponse.class, result);
        }
    }

    @Nested
    @DisplayName("CT02 - Phase 6: Merchant ID Numeric Check (BR-AT-11)")
    class Phase6Tests {

        @BeforeEach
        void setUpXref() {
            lenient().when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));
        }

        @Test
        @DisplayName("BR-AT-11: Non-numeric merchant ID -> exact message")
        void testPhase6_NonNumericMerchantId() {
            AddTransactionRequest request = buildValidRequest();
            request.setMerchantId("ABC123");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Merchant ID must be Numeric...", ex.getMessage());
            assertEquals(6, ex.getPhase());
            assertEquals("merchantId", ex.getField());
            assertEquals("BR-AT-11", ex.getBusinessRule());
        }

        @Test
        @DisplayName("BR-AT-11: Merchant ID with special characters")
        void testPhase6_MerchantIdSpecialChars() {
            AddTransactionRequest request = buildValidRequest();
            request.setMerchantId("100-000-001");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Merchant ID must be Numeric...", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("CT02 - Confirmation Gate (BR-AT-12)")
    class ConfirmationGateTests {

        @BeforeEach
        void setUpXref() {
            lenient().when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));
        }

        @Test
        @DisplayName("BR-AT-12: Confirmation 'Y' creates transaction")
        void testConfirmation_Y_CreatesTransaction() {
            AddTransactionRequest request = buildValidRequest();
            request.setConfirmation("Y");
            when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));
            when(transactionRepository.generateNextTransactionId())
                    .thenReturn("0000000000000016");
            when(transactionRepository.existsByTransactionId("0000000000000016"))
                    .thenReturn(false);
            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Object result = transactionService.addTransaction(request);

            assertInstanceOf(AddTransactionResponse.class, result);
            AddTransactionResponse addResp = (AddTransactionResponse) result;
            assertEquals("0000000000000016", addResp.getTransactionId());
            assertTrue(addResp.getMessage().contains("Transaction added successfully"));
            assertTrue(addResp.getMessage().contains("0000000000000016"));
        }

        @Test
        @DisplayName("BR-AT-12: Confirmation 'y' (lowercase) creates transaction")
        void testConfirmation_y_LowerCase_CreatesTransaction() {
            AddTransactionRequest request = buildValidRequest();
            request.setConfirmation("y");
            when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));
            when(transactionRepository.generateNextTransactionId())
                    .thenReturn("0000000000000017");
            when(transactionRepository.existsByTransactionId("0000000000000017"))
                    .thenReturn(false);
            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Object result = transactionService.addTransaction(request);

            assertInstanceOf(AddTransactionResponse.class, result);
        }

        @Test
        @DisplayName("BR-AT-12: Confirmation 'N' returns confirmation required")
        void testConfirmation_N_ReturnsConfirmation() {
            AddTransactionRequest request = buildValidRequest();
            request.setConfirmation("N");
            when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));

            Object result = transactionService.addTransaction(request);

            assertInstanceOf(ConfirmationRequiredResponse.class, result);
            ConfirmationRequiredResponse confirmResp = (ConfirmationRequiredResponse) result;
            assertEquals("Confirm to add this transaction...", confirmResp.getMessage());
            assertTrue(confirmResp.isConfirmationRequired());
        }

        @Test
        @DisplayName("BR-AT-12: Confirmation 'n' (lowercase) returns confirmation required")
        void testConfirmation_n_LowerCase_ReturnsConfirmation() {
            AddTransactionRequest request = buildValidRequest();
            request.setConfirmation("n");
            when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));

            Object result = transactionService.addTransaction(request);

            assertInstanceOf(ConfirmationRequiredResponse.class, result);
            ConfirmationRequiredResponse confirmResp = (ConfirmationRequiredResponse) result;
            assertEquals("Confirm to add this transaction...", confirmResp.getMessage());
        }

        @Test
        @DisplayName("BR-AT-12: Null confirmation returns confirmation required")
        void testConfirmation_Null_ReturnsConfirmation() {
            AddTransactionRequest request = buildValidRequest();
            request.setConfirmation(null);
            when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));

            Object result = transactionService.addTransaction(request);

            assertInstanceOf(ConfirmationRequiredResponse.class, result);
        }

        @Test
        @DisplayName("BR-AT-12: Empty confirmation returns confirmation required")
        void testConfirmation_Empty_ReturnsConfirmation() {
            AddTransactionRequest request = buildValidRequest();
            request.setConfirmation("");
            when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));

            Object result = transactionService.addTransaction(request);

            assertInstanceOf(ConfirmationRequiredResponse.class, result);
        }

        @Test
        @DisplayName("BR-AT-12: Blank confirmation returns confirmation required")
        void testConfirmation_Blank_ReturnsConfirmation() {
            AddTransactionRequest request = buildValidRequest();
            request.setConfirmation("   ");
            when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));

            Object result = transactionService.addTransaction(request);

            assertInstanceOf(ConfirmationRequiredResponse.class, result);
        }

        @Test
        @DisplayName("BR-AT-12: Invalid confirmation value -> exact message")
        void testConfirmation_Invalid_ReturnsInvalidMessage() {
            AddTransactionRequest request = buildValidRequest();
            request.setConfirmation("X");
            when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));

            Object result = transactionService.addTransaction(request);

            assertInstanceOf(ConfirmationRequiredResponse.class, result);
            ConfirmationRequiredResponse confirmResp = (ConfirmationRequiredResponse) result;
            assertEquals("Invalid value. Valid values are (Y/N)...", confirmResp.getMessage());
        }

        @Test
        @DisplayName("BR-AT-12: Multi-character invalid confirmation -> exact message")
        void testConfirmation_MultiChar_ReturnsInvalidMessage() {
            AddTransactionRequest request = buildValidRequest();
            request.setConfirmation("YES");
            when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));

            Object result = transactionService.addTransaction(request);

            assertInstanceOf(ConfirmationRequiredResponse.class, result);
            ConfirmationRequiredResponse confirmResp = (ConfirmationRequiredResponse) result;
            assertEquals("Invalid value. Valid values are (Y/N)...", confirmResp.getMessage());
        }
    }

    @Nested
    @DisplayName("CT02 - ID Generation & Duplicate Handling (BR-AT-13, BR-AT-14)")
    class IdGenerationTests {

        @BeforeEach
        void setUpXref() {
            lenient().when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));
        }

        @Test
        @DisplayName("BR-AT-13: Generates sequential ID via repository")
        void testIdGeneration_UsesSequence() {
            AddTransactionRequest request = buildValidRequest();
            request.setConfirmation("Y");
            when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));
            when(transactionRepository.generateNextTransactionId())
                    .thenReturn("0000000000000016");
            when(transactionRepository.existsByTransactionId("0000000000000016"))
                    .thenReturn(false);
            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Object result = transactionService.addTransaction(request);

            verify(transactionRepository).generateNextTransactionId();
            assertInstanceOf(AddTransactionResponse.class, result);
            assertEquals("0000000000000016", ((AddTransactionResponse) result).getTransactionId());
        }

        @Test
        @DisplayName("BR-AT-14: Duplicate ID detected before save -> DuplicateTransactionException")
        void testIdGeneration_DuplicateBeforeSave() {
            AddTransactionRequest request = buildValidRequest();
            request.setConfirmation("Y");
            when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));
            when(transactionRepository.generateNextTransactionId())
                    .thenReturn("0000000000000001");
            when(transactionRepository.existsByTransactionId("0000000000000001"))
                    .thenReturn(true);

            DuplicateTransactionException ex = assertThrows(DuplicateTransactionException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Tran ID already exist...", ex.getMessage());
        }

        @Test
        @DisplayName("BR-AT-14: DataIntegrityViolation with 'duplicate' -> DuplicateTransactionException")
        void testIdGeneration_DataIntegrityDuplicate() {
            AddTransactionRequest request = buildValidRequest();
            request.setConfirmation("Y");
            when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));
            when(transactionRepository.generateNextTransactionId())
                    .thenReturn("0000000000000016");
            when(transactionRepository.existsByTransactionId("0000000000000016"))
                    .thenReturn(false);
            when(transactionRepository.save(any(Transaction.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate key value"));

            DuplicateTransactionException ex = assertThrows(DuplicateTransactionException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Tran ID already exist...", ex.getMessage());
        }

        @Test
        @DisplayName("DataIntegrityViolation without 'duplicate' -> RuntimeException")
        void testIdGeneration_DataIntegrityOther() {
            AddTransactionRequest request = buildValidRequest();
            request.setConfirmation("Y");
            when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));
            when(transactionRepository.generateNextTransactionId())
                    .thenReturn("0000000000000016");
            when(transactionRepository.existsByTransactionId("0000000000000016"))
                    .thenReturn(false);
            when(transactionRepository.save(any(Transaction.class)))
                    .thenThrow(new DataIntegrityViolationException("foreign key violation"));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals("Unable to Add Transaction...", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("CT02 - Full Validation Chain Order Tests")
    class FullChainTests {

        @Test
        @DisplayName("Phase 1 failure prevents Phase 2 execution")
        void testChain_Phase1FailsPreventsPhase2() {
            AddTransactionRequest request = new AddTransactionRequest();
            // No account, no card -> Phase 1 fails at BR-AT-01
            // All data fields also empty -> would fail Phase 2 if reached

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals(1, ex.getPhase());
            assertEquals("BR-AT-01", ex.getBusinessRule());
        }

        @Test
        @DisplayName("Phase 2 failure prevents Phase 3 execution")
        void testChain_Phase2FailsPreventsPhase3() {
            AddTransactionRequest request = buildValidRequest();
            request.setTypeCode(null); // Phase 2 will fail
            // typeCode "AB" would fail Phase 3 if reached
            when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals(2, ex.getPhase());
        }

        @Test
        @DisplayName("Phase 3 failure prevents Phase 4 execution")
        void testChain_Phase3FailsPreventsPhase4() {
            AddTransactionRequest request = buildValidRequest();
            request.setTypeCode("AB"); // Phase 3 will fail
            request.setAmount("invalid"); // Would fail Phase 4 if reached
            when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals(3, ex.getPhase());
        }

        @Test
        @DisplayName("Phase 4 failure prevents Phase 5 execution")
        void testChain_Phase4FailsPreventsPhase5() {
            AddTransactionRequest request = buildValidRequest();
            request.setAmount("invalid"); // Phase 4 will fail
            request.setOriginationDate("bad-date"); // Would fail Phase 5 if reached
            when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals(4, ex.getPhase());
        }

        @Test
        @DisplayName("Phase 5 failure prevents Phase 6 execution")
        void testChain_Phase5FailsPreventsPhase6() {
            AddTransactionRequest request = buildValidRequest();
            request.setOriginationDate("01-01-2024"); // Phase 5 will fail
            request.setMerchantId("ABC"); // Would fail Phase 6 if reached
            when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> transactionService.addTransaction(request));

            assertEquals(5, ex.getPhase());
        }

        @Test
        @DisplayName("All phases pass with valid data + confirmation Y -> 201 Created")
        void testChain_AllPhasesPass_ConfirmY() {
            AddTransactionRequest request = buildValidRequest();
            request.setConfirmation("Y");
            when(xrefRepository.findFirstByAccountId(any(BigDecimal.class)))
                    .thenReturn(Optional.of(sampleXref));
            when(transactionRepository.generateNextTransactionId())
                    .thenReturn("0000000000000016");
            when(transactionRepository.existsByTransactionId("0000000000000016"))
                    .thenReturn(false);
            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Object result = transactionService.addTransaction(request);

            assertInstanceOf(AddTransactionResponse.class, result);
            AddTransactionResponse resp = (AddTransactionResponse) result;
            assertEquals("0000000000000016", resp.getTransactionId());
            assertNotNull(resp.getTransaction());
        }
    }

    // ========================================================================
    // PF5 - Copy Last Transaction
    // ========================================================================

    @Nested
    @DisplayName("PF5 - Latest Transaction (Copy Last)")
    class LatestTransactionTests {

        @Test
        @DisplayName("Returns latest transaction data")
        void testGetLatestTransaction_ReturnsData() {
            when(transactionRepository.findFirstByOrderByTransactionIdDesc())
                    .thenReturn(Optional.of(sampleTransaction));

            LatestTransactionResponse response = transactionService.getLatestTransaction();

            assertNotNull(response);
            assertEquals("0000000000000001", response.getTransactionId());
            assertEquals("01", response.getTypeCode());
            assertEquals(5001, response.getCategoryCode());
            assertEquals("ONLINE", response.getSource());
            assertEquals("Monthly Subscription Service", response.getDescription());
            assertEquals(new BigDecimal("-14.99"), response.getAmount());
            assertEquals(100000001L, response.getMerchantId());
            assertEquals("StreamFlix", response.getMerchantName());
            assertEquals("Los Angeles", response.getMerchantCity());
            assertEquals("90001", response.getMerchantZip());
        }

        @Test
        @DisplayName("No transactions found throws ResourceNotFoundException")
        void testGetLatestTransaction_NoTransactions() {
            when(transactionRepository.findFirstByOrderByTransactionIdDesc())
                    .thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> transactionService.getLatestTransaction());

            assertEquals("No transactions found", ex.getMessage());
        }
    }
}
