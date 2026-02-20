package com.carddemo.service;

import com.carddemo.dto.BillPaymentRequest;
import com.carddemo.entity.Account;
import com.carddemo.entity.CardCrossReference;
import com.carddemo.entity.Transaction;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardCrossReferenceRepository;
import com.carddemo.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BillPaymentService {

    private final AccountRepository accountRepository;
    private final CardCrossReferenceRepository cardCrossReferenceRepository;
    private final TransactionRepository transactionRepository;

    public BillPaymentService(AccountRepository accountRepository,
                              CardCrossReferenceRepository cardCrossReferenceRepository,
                              TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.cardCrossReferenceRepository = cardCrossReferenceRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Transaction processBillPayment(BillPaymentRequest request) {
        Account account = accountRepository.findById(request.getAcctId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: " + request.getAcctId()));

        if (!"Y".equals(account.getActiveStatus())) {
            throw new IllegalArgumentException("Account is not active");
        }

        List<CardCrossReference> xrefs = cardCrossReferenceRepository.findByAcctId(request.getAcctId());
        if (xrefs.isEmpty()) {
            throw new ResourceNotFoundException("No card found for account: " + request.getAcctId());
        }

        account.setCurrBal(account.getCurrBal().subtract(request.getAmount()));
        account.setCurrCycCredit(
                account.getCurrCycCredit() != null
                        ? account.getCurrCycCredit().add(request.getAmount())
                        : request.getAmount());
        accountRepository.save(account);

        String tranId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Transaction transaction = new Transaction();
        transaction.setTranId(tranId);
        transaction.setTypeCd("02");
        transaction.setCatCd(1);
        transaction.setSource("ONLINE");
        transaction.setDescription("Bill Payment");
        transaction.setAmount(request.getAmount().negate());
        transaction.setCardNum(xrefs.get(0).getCardNum());
        transaction.setOrigTs(LocalDateTime.now());
        transaction.setProcTs(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }
}
