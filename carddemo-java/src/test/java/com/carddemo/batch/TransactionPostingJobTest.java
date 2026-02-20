package com.carddemo.batch;

import com.carddemo.entity.Account;
import com.carddemo.entity.CardCrossReference;
import com.carddemo.entity.Transaction;
import com.carddemo.entity.TransactionCategoryBalance;
import com.carddemo.entity.TransactionCategoryBalanceId;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardCrossReferenceRepository;
import com.carddemo.repository.TransactionCategoryBalanceRepository;
import com.carddemo.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionPostingJobTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CardCrossReferenceRepository cardCrossReferenceRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionCategoryBalanceRepository tranCatBalRepository;

    @InjectMocks
    private TransactionPostingJob transactionPostingJob;

    private Account testAccount;
    private CardCrossReference testXref;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setAcctId(1L);
        testAccount.setActiveStatus("Y");
        testAccount.setCurrBal(new BigDecimal("1000.00"));
        testAccount.setCurrCycDebit(BigDecimal.ZERO);
        testAccount.setCurrCycCredit(BigDecimal.ZERO);

        testXref = new CardCrossReference();
        testXref.setCardNum("9680294154603697");
        testXref.setCustId(1L);
        testXref.setAcctId(1L);
    }

    @Test
    void executeWithValidTransactions() {
        when(cardCrossReferenceRepository.findById("9680294154603697"))
                .thenReturn(Optional.of(testXref));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(tranCatBalRepository.findById(any(TransactionCategoryBalanceId.class)))
                .thenReturn(Optional.empty());
        when(tranCatBalRepository.save(any(TransactionCategoryBalance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction tran = new Transaction();
        tran.setTranId("TEST000000000001");
        tran.setTypeCd("01");
        tran.setCatCd(1);
        tran.setAmount(new BigDecimal("50.00"));
        tran.setCardNum("9680294154603697");
        tran.setOrigTs(LocalDateTime.now());

        TransactionPostingJob.BatchResult result =
                transactionPostingJob.execute(List.of(tran));

        assertEquals(1, result.processedCount());
        assertEquals(0, result.rejectedCount());
    }

    @Test
    void executeWithInvalidCardNumber() {
        when(cardCrossReferenceRepository.findById("0000000000000000"))
                .thenReturn(Optional.empty());

        Transaction tran = new Transaction();
        tran.setTranId("TEST000000000002");
        tran.setTypeCd("01");
        tran.setCatCd(1);
        tran.setAmount(new BigDecimal("50.00"));
        tran.setCardNum("0000000000000000");

        TransactionPostingJob.BatchResult result =
                transactionPostingJob.execute(List.of(tran));

        assertEquals(0, result.processedCount());
        assertEquals(1, result.rejectedCount());
    }
}
