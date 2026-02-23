package com.carddemo.api.service;

import com.carddemo.api.dto.PaymentRequest;
import com.carddemo.api.dto.TransactionResponse;
import com.carddemo.core.domain.Account;
import com.carddemo.core.domain.Card;
import com.carddemo.core.domain.Transaction;
import com.carddemo.core.exception.BusinessValidationException;
import com.carddemo.core.exception.InsufficientFundsException;
import com.carddemo.core.exception.ResourceNotFoundException;
import com.carddemo.core.repository.AccountRepository;
import com.carddemo.core.repository.CardRepository;
import com.carddemo.core.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for Payment (Bill Pay) operations.
 * Replaces business logic from COBIL00C (Bill Payment).
 *
 * Key COBOL logic replaced:
 * - Balance check: WS-RETURN-MSG if balance insufficient
 * - Account update: Decrease current balance after payment
 * - Transaction creation: Write payment record to TRANSACT file
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public TransactionResponse processPayment(PaymentRequest request) {
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account",
                        String.valueOf(request.getAccountId())));

        // Find a card for this account
        List<Card> cards = cardRepository.findByAcctId(account.getAcctId());
        if (cards.isEmpty()) {
            throw new BusinessValidationException("No card found for account: " + account.getAcctId());
        }

        // Check sufficient balance (replaces COBOL balance check in COBIL00C)
        if (account.getCurrentBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(request.getAmount(), account.getCurrentBalance());
        }

        // Update account balance
        account.setCurrentBalance(account.getCurrentBalance().subtract(request.getAmount()));
        account.setCurrentCycleDebit(
                account.getCurrentCycleDebit().add(request.getAmount()));
        accountRepository.save(account);

        // Create payment transaction record
        Transaction txn = Transaction.builder()
                .tranId(UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                .typeCode("PM")
                .categoryCode(0)
                .source("ONLINE")
                .description(request.getDescription() != null ? request.getDescription() : "Bill Payment")
                .amount(request.getAmount())
                .cardNum(cards.get(0).getCardNum())
                .origTimestamp(LocalDateTime.now())
                .procTimestamp(LocalDateTime.now())
                .build();

        Transaction saved = transactionRepository.save(txn);

        return TransactionResponse.builder()
                .transactionId(saved.getTranId())
                .typeCode(saved.getTypeCode())
                .amount(saved.getAmount())
                .description(saved.getDescription())
                .cardNumber(saved.getCardNum())
                .originTimestamp(saved.getOrigTimestamp())
                .processTimestamp(saved.getProcTimestamp())
                .build();
    }
}
