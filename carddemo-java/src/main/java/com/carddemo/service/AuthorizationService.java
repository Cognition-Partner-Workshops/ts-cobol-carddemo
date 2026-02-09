package com.carddemo.service;

import com.carddemo.dto.AuthorizationRequest;
import com.carddemo.entity.Account;
import com.carddemo.entity.AuthFraud;
import com.carddemo.entity.AuthorizationDetail;
import com.carddemo.entity.AuthorizationSummary;
import com.carddemo.entity.Card;
import com.carddemo.entity.CardAccountXref;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.exception.ValidationException;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.AuthFraudRepository;
import com.carddemo.repository.AuthorizationDetailRepository;
import com.carddemo.repository.AuthorizationSummaryRepository;
import com.carddemo.repository.CardAccountXrefRepository;
import com.carddemo.repository.CardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class AuthorizationService {

    private final AuthorizationSummaryRepository summaryRepository;
    private final AuthorizationDetailRepository detailRepository;
    private final AuthFraudRepository fraudRepository;
    private final CardRepository cardRepository;
    private final CardAccountXrefRepository xrefRepository;
    private final AccountRepository accountRepository;

    public AuthorizationService(AuthorizationSummaryRepository summaryRepository,
                                AuthorizationDetailRepository detailRepository,
                                AuthFraudRepository fraudRepository,
                                CardRepository cardRepository,
                                CardAccountXrefRepository xrefRepository,
                                AccountRepository accountRepository) {
        this.summaryRepository = summaryRepository;
        this.detailRepository = detailRepository;
        this.fraudRepository = fraudRepository;
        this.cardRepository = cardRepository;
        this.xrefRepository = xrefRepository;
        this.accountRepository = accountRepository;
    }

    public AuthorizationDetail processAuthorization(AuthorizationRequest request) {
        Card card = cardRepository.findById(request.getCardNum())
                .orElseThrow(() -> new ValidationException("Card not found: " + request.getCardNum()));

        if (!"Y".equals(card.getActiveStatus())) {
            throw new ValidationException("Card is not active");
        }

        CardAccountXref xref = xrefRepository.findById(request.getCardNum())
                .orElseThrow(() -> new ValidationException("Card cross reference not found"));

        Account account = accountRepository.findById(xref.getAcctId())
                .orElseThrow(() -> new ValidationException("Account not found for card"));

        if (!"Y".equals(account.getActiveStatus())) {
            throw new ValidationException("Account is not active");
        }

        BigDecimal availableCredit = account.getCreditLimit().subtract(account.getCurrBal());
        if (request.getAmount().compareTo(availableCredit) > 0) {
            throw new ValidationException("Authorization declined: insufficient credit");
        }

        AuthorizationSummary summary = summaryRepository.findByCardNum(request.getCardNum())
                .orElseGet(() -> {
                    AuthorizationSummary newSummary = new AuthorizationSummary();
                    newSummary.setCardNum(request.getCardNum());
                    newSummary.setAcctId(xref.getAcctId());
                    newSummary.setTotalAuthAmt(BigDecimal.ZERO);
                    newSummary.setAuthCount(0);
                    return newSummary;
                });

        summary.setTotalAuthAmt(summary.getTotalAuthAmt().add(request.getAmount()));
        summary.setAuthCount(summary.getAuthCount() + 1);
        summary.setLastAuthDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        summaryRepository.save(summary);

        AuthorizationDetail detail = new AuthorizationDetail();
        detail.setAuthId(summary.getAuthId());
        detail.setAuthDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        detail.setAuthTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        detail.setAuthAmount(request.getAmount());
        detail.setAuthStatus("A");
        detail.setMerchantId(request.getMerchantId());
        detail.setMerchantName(request.getMerchantName());

        return detailRepository.save(detail);
    }

    @Transactional(readOnly = true)
    public AuthorizationSummary getAuthorizationSummary(String cardNum) {
        return summaryRepository.findByCardNum(cardNum)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No authorization summary found for card: " + cardNum));
    }

    @Transactional(readOnly = true)
    public List<AuthorizationDetail> getAuthorizationDetails(Long authId) {
        return detailRepository.findByAuthIdOrderByAuthDateDescAuthTimeDesc(authId);
    }

    public AuthFraud markAsFraud(String cardNum, BigDecimal amount, String reason) {
        CardAccountXref xref = xrefRepository.findById(cardNum)
                .orElseThrow(() -> new ValidationException("Card cross reference not found"));

        AuthFraud fraud = new AuthFraud();
        fraud.setCardNum(cardNum);
        fraud.setAcctId(xref.getAcctId());
        fraud.setFraudDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        fraud.setFraudAmount(amount);
        fraud.setFraudReason(reason);

        return fraudRepository.save(fraud);
    }

    public void purgeExpiredAuthorizations(String cutoffDate) {
        List<AuthorizationSummary> summaries = summaryRepository.findAll();
        for (AuthorizationSummary summary : summaries) {
            if (summary.getLastAuthDate() != null && summary.getLastAuthDate().compareTo(cutoffDate) < 0) {
                List<AuthorizationDetail> details =
                        detailRepository.findByAuthIdOrderByAuthDateDescAuthTimeDesc(summary.getAuthId());
                detailRepository.deleteAll(details);
                summaryRepository.delete(summary);
            }
        }
    }
}
