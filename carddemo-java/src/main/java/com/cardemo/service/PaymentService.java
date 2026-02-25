package com.cardemo.service;

import com.cardemo.dto.PaymentRequest;
import com.cardemo.entity.Account;
import com.cardemo.entity.Transaction;
import com.cardemo.exception.CardDemoException;
import com.cardemo.repository.AccountRepository;
import com.cardemo.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Payment processing service.
 * Migrated from COBIL00C (CB00 transaction) - Bill Payment.
 * COBOL flow: PROCESS-ENTER-KEY -> READ account -> validate -> WRITE payment transaction -> UPDATE account balance
 */
@Service
public class PaymentService {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS");
    private static final String PAYMENT_TRAN_TYPE = "04";
    private static final int PAYMENT_TRAN_CAT = 1;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public PaymentService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Process payment - migrated from COBIL00C (CB00 transaction).
     * COBOL: PROCESS-ENTER-KEY paragraph
     *   READ ACCTFILE -> validate account active
     *   Validate payment amount > 0
     *   COMPUTE ACCT-CURR-BAL = ACCT-CURR-BAL - WS-PAYMENT-AMT
     *   WRITE transaction record
     *   REWRITE account record
     */
    @Transactional
    public Transaction processPayment(PaymentRequest request) {
        // Read account - COBOL: EXEC CICS READ DATASET(WS-ACCTFILENAME)
        Account account = accountRepository.findById(request.getAcctId())
                .orElseThrow(() -> CardDemoException.notFound("Account not found: " + request.getAcctId()));

        // Validate account is active
        if (!"Y".equalsIgnoreCase(account.getAcctActiveStatus())) {
            throw CardDemoException.badRequest("Account is not active");
        }

        // Validate payment amount - COBOL: IF WS-PAYMENT-AMT <= ZERO
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw CardDemoException.badRequest("Payment amount must be greater than zero");
        }

        // Create payment transaction record
        String tranId = String.format("%016d", System.currentTimeMillis());
        String now = LocalDateTime.now().format(TS_FORMAT);

        Transaction paymentTran = new Transaction();
        paymentTran.setTranId(tranId);
        paymentTran.setTranTypeCd(PAYMENT_TRAN_TYPE);
        paymentTran.setTranCatCd(PAYMENT_TRAN_CAT);
        paymentTran.setTranSource("ONLINE");
        paymentTran.setTranDesc(request.getDescription() != null ? request.getDescription() : "Payment");
        paymentTran.setTranAmt(request.getAmount().negate()); // Payments reduce balance
        paymentTran.setTranCardNum(request.getCardNum());
        paymentTran.setTranOrigTs(now);
        paymentTran.setTranProcTs(now);

        Transaction saved = transactionRepository.save(paymentTran);

        // Update account balance - COBOL: COMPUTE ACCT-CURR-BAL = ACCT-CURR-BAL - WS-PAYMENT-AMT
        account.setAcctCurrBal(account.getAcctCurrBal().subtract(request.getAmount()));
        account.setAcctCurrCycCredit(account.getAcctCurrCycCredit().add(request.getAmount()));
        accountRepository.save(account);

        return saved;
    }
}
