package com.carddemo.service;

import com.carddemo.dto.BillPaymentRequest;
import com.carddemo.entity.Account;
import com.carddemo.entity.CardCrossReference;
import com.carddemo.entity.Transaction;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardCrossReferenceRepository;
import com.carddemo.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillPaymentServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CardCrossReferenceRepository cardCrossReferenceRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BillPaymentService billPaymentService;

    private Account testAccount;
    private CardCrossReference testXref;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setAcctId(1L);
        testAccount.setActiveStatus("Y");
        testAccount.setCurrBal(new BigDecimal("1940.00"));
        testAccount.setCurrCycCredit(BigDecimal.ZERO);

        testXref = new CardCrossReference();
        testXref.setCardNum("9680294154603697");
        testXref.setCustId(1L);
        testXref.setAcctId(1L);
    }

    @Test
    void processBillPaymentSuccess() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(cardCrossReferenceRepository.findByAcctId(1L)).thenReturn(List.of(testXref));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BillPaymentRequest request = new BillPaymentRequest();
        request.setAcctId(1L);
        request.setAmount(new BigDecimal("500.00"));

        Transaction result = billPaymentService.processBillPayment(request);

        assertNotNull(result);
        assertEquals("02", result.getTypeCd());
        assertEquals(new BigDecimal("-500.00"), result.getAmount());
    }

    @Test
    void processBillPaymentAccountNotFound() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        BillPaymentRequest request = new BillPaymentRequest();
        request.setAcctId(999L);
        request.setAmount(new BigDecimal("500.00"));

        assertThrows(ResourceNotFoundException.class,
                () -> billPaymentService.processBillPayment(request));
    }

    @Test
    void processBillPaymentInactiveAccount() {
        testAccount.setActiveStatus("N");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        BillPaymentRequest request = new BillPaymentRequest();
        request.setAcctId(1L);
        request.setAmount(new BigDecimal("500.00"));

        assertThrows(IllegalArgumentException.class,
                () -> billPaymentService.processBillPayment(request));
    }
}
