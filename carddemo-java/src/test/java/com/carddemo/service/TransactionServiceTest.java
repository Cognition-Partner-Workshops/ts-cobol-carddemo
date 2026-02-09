package com.carddemo.service;

import com.carddemo.dto.PaymentRequest;
import com.carddemo.dto.TransactionDto;
import com.carddemo.entity.Account;
import com.carddemo.entity.CardAccountXref;
import com.carddemo.entity.Transaction;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardAccountXrefRepository;
import com.carddemo.repository.TransactionCategoryBalanceRepository;
import com.carddemo.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private CardAccountXrefRepository xrefRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionCategoryBalanceRepository tcatBalRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testTransaction = new Transaction();
        testTransaction.setTranId("T0000000000001");
        testTransaction.setCardNum("4111111111111111");
        testTransaction.setTypeCd("PR");
        testTransaction.setCatCd(5001);
        testTransaction.setSource("POS");
        testTransaction.setDescription("Test Purchase");
        testTransaction.setAmount(new BigDecimal("99.99"));
        testTransaction.setMerchantName("Test Store");
        testTransaction.setMerchantCity("New York");
        testTransaction.setMerchantZip("10001");
    }

    @Test
    void listTransactions_returnsPagedResults() {
        Page<Transaction> page = new PageImpl<>(List.of(testTransaction));
        when(transactionRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<Transaction> result = transactionService.listTransactions(PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("T0000000000001", result.getContent().get(0).getTranId());
    }

    @Test
    void getTransaction_existingTran_returnsTransaction() {
        when(transactionRepository.findById("T0000000000001")).thenReturn(Optional.of(testTransaction));

        Transaction result = transactionService.getTransaction("T0000000000001");

        assertNotNull(result);
        assertEquals("Test Purchase", result.getDescription());
    }

    @Test
    void getTransaction_nonExistent_throwsResourceNotFoundException() {
        when(transactionRepository.findById("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.getTransaction("UNKNOWN"));
    }

    @Test
    void addTransaction_validDto_createsTransaction() {
        TransactionDto dto = new TransactionDto();
        dto.setCardNum("4111111111111111");
        dto.setTypeCd("PR");
        dto.setCatCd(5001);
        dto.setSource("POS");
        dto.setDescription("New Purchase");
        dto.setAmount(new BigDecimal("49.99"));

        CardAccountXref xref = new CardAccountXref();
        xref.setCardNum("4111111111111111");
        xref.setAcctId(1000000001L);

        Account account = new Account();
        account.setAcctId(1000000001L);
        account.setActiveStatus("Y");
        account.setCurrBal(new BigDecimal("1500.00"));
        account.setCreditLimit(new BigDecimal("5000.00"));
        account.setCurrCycDebit(BigDecimal.ZERO);

        when(xrefRepository.findById("4111111111111111")).thenReturn(Optional.of(xref));
        when(accountRepository.findById(1000000001L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tcatBalRepository.findById(any())).thenReturn(Optional.empty());
        when(tcatBalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            return t;
        });

        Transaction result = transactionService.addTransaction(dto);

        assertNotNull(result);
        assertEquals("New Purchase", result.getDescription());
        assertEquals(new BigDecimal("49.99"), result.getAmount());
    }

    @Test
    void processPayment_validRequest_createsPaymentTransaction() {
        PaymentRequest request = new PaymentRequest();
        request.setAcctId(1000000001L);
        request.setCardNum("4111111111111111");
        request.setAmount(new BigDecimal("500.00"));

        CardAccountXref xref = new CardAccountXref();
        xref.setCardNum("4111111111111111");
        xref.setAcctId(1000000001L);

        Account account = new Account();
        account.setAcctId(1000000001L);
        account.setActiveStatus("Y");
        account.setCurrBal(new BigDecimal("1500.00"));
        account.setCreditLimit(new BigDecimal("5000.00"));
        account.setCurrCycCredit(BigDecimal.ZERO);

        when(xrefRepository.findById("4111111111111111")).thenReturn(Optional.of(xref));
        when(accountRepository.findById(1000000001L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tcatBalRepository.findById(any())).thenReturn(Optional.empty());
        when(tcatBalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction result = transactionService.processPayment(request);

        assertNotNull(result);
        assertEquals(new BigDecimal("-500.00"), result.getAmount());
    }

    @Test
    void listTransactionsByCard_returnsFilteredResults() {
        Page<Transaction> page = new PageImpl<>(List.of(testTransaction));
        when(transactionRepository.findByCardNum(eq("4111111111111111"), any(PageRequest.class)))
                .thenReturn(page);

        Page<Transaction> result = transactionService.listTransactionsByCard(
                "4111111111111111", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }
}
