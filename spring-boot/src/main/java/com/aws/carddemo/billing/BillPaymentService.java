package com.aws.carddemo.billing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aws.carddemo.account.Account;
import com.aws.carddemo.account.AccountRepository;
import com.aws.carddemo.billing.dto.BalanceResponse;
import com.aws.carddemo.billing.dto.BillPaymentRequest;
import com.aws.carddemo.billing.dto.BillPaymentResponse;
import com.aws.carddemo.card.Card;
import com.aws.carddemo.card.CardRepository;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.exception.ValidationException;
import com.aws.carddemo.transaction.TransactionRecord;
import com.aws.carddemo.transaction.TransactionRecordRepository;

@Service
@Transactional
public class BillPaymentService {

    private static final String PAYMENT_TYPE_CODE = "BP";
    private static final String PAYMENT_SOURCE = "ONLINE-PAY";
    private static final String PAYMENT_DESCRIPTION = "Bill Payment";
    private static final String ACTIVE_STATUS = "A";

    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final TransactionRecordRepository transactionRecordRepository;

    public BillPaymentService(AccountRepository accountRepository,
                              CardRepository cardRepository,
                              TransactionRecordRepository transactionRecordRepository) {
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.transactionRecordRepository = transactionRecordRepository;
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        return new BalanceResponse(
                account.getId(),
                account.getCurrentBalance(),
                account.getAccountStatus()
        );
    }

    public BillPaymentResponse processPayment(BillPaymentRequest request) {
        Account account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with id: " + request.accountId()));

        if (!ACTIVE_STATUS.equals(account.getAccountStatus())) {
            throw new ValidationException("Account is not active");
        }

        if (!Boolean.TRUE.equals(request.confirmed())) {
            throw new ValidationException("Payment must be confirmed");
        }

        BigDecimal currentBalance = account.getCurrentBalance();
        if (currentBalance.compareTo(BigDecimal.ZERO) == 0) {
            throw new ValidationException("Account has zero balance; nothing to pay");
        }

        Card card = findCardForAccount(account.getId());

        LocalDateTime now = LocalDateTime.now();

        TransactionRecord transaction = new TransactionRecord();
        transaction.setCard(card);
        transaction.setTransactionType(PAYMENT_TYPE_CODE);
        transaction.setTransactionSource(PAYMENT_SOURCE);
        transaction.setDescription(PAYMENT_DESCRIPTION);
        transaction.setAmount(currentBalance.negate());
        transaction.setTimestamp(now);
        TransactionRecord savedTransaction = transactionRecordRepository.save(transaction);

        account.setCurrentBalance(BigDecimal.ZERO);
        accountRepository.save(account);

        return new BillPaymentResponse(
                account.getId(),
                currentBalance,
                BigDecimal.ZERO,
                savedTransaction.getId(),
                now
        );
    }

    private Card findCardForAccount(Long accountId) {
        List<Card> cards = cardRepository.findByAccountId(accountId);
        if (cards.isEmpty()) {
            throw new ResourceNotFoundException("No card found for account id: " + accountId);
        }
        return cards.get(0);
    }
}
