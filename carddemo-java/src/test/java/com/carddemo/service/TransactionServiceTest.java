package com.carddemo.service;

import com.carddemo.dto.TransactionRequest;
import com.carddemo.entity.CardCrossReference;
import com.carddemo.entity.Transaction;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.repository.CardCrossReferenceRepository;
import com.carddemo.repository.TransactionCategoryRepository;
import com.carddemo.repository.TransactionRepository;
import com.carddemo.repository.TransactionTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CardCrossReferenceRepository cardCrossReferenceRepository;

    @Mock
    private TransactionTypeRepository transactionTypeRepository;

    @Mock
    private TransactionCategoryRepository transactionCategoryRepository;

    @InjectMocks
    private TransactionService transactionService;

    private CardCrossReference testXref;

    @BeforeEach
    void setUp() {
        testXref = new CardCrossReference();
        testXref.setCardNum("9680294154603697");
        testXref.setCustId(1L);
        testXref.setAcctId(1L);
    }

    @Test
    void addTransactionSuccess() {
        when(cardCrossReferenceRepository.findById("9680294154603697"))
                .thenReturn(Optional.of(testXref));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionRequest request = new TransactionRequest();
        request.setCardNum("9680294154603697");
        request.setTypeCd("01");
        request.setCatCd(1);
        request.setAmount(new BigDecimal("50.00"));
        request.setMerchantName("Test Store");

        Transaction result = transactionService.addTransaction(request);

        assertNotNull(result);
        assertNotNull(result.getTranId());
        assertEquals("01", result.getTypeCd());
        assertEquals(new BigDecimal("50.00"), result.getAmount());
        assertEquals("9680294154603697", result.getCardNum());
    }

    @Test
    void addTransactionCardNotFound() {
        when(cardCrossReferenceRepository.findById("0000000000000000"))
                .thenReturn(Optional.empty());

        TransactionRequest request = new TransactionRequest();
        request.setCardNum("0000000000000000");
        request.setTypeCd("01");
        request.setCatCd(1);
        request.setAmount(new BigDecimal("50.00"));

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.addTransaction(request));
    }

    @Test
    void getTransactionNotFound() {
        when(transactionRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.getTransaction("nonexistent"));
    }
}
