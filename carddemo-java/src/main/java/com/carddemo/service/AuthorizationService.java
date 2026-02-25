package com.carddemo.service;

import com.carddemo.dto.AuthorizationRequest;
import com.carddemo.entity.Account;
import com.carddemo.entity.AuthorizationDetail;
import com.carddemo.entity.AuthorizationSummary;
import com.carddemo.entity.Card;
import com.carddemo.entity.CardAccountXref;
import com.carddemo.entity.FraudRecord;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.AuthorizationDetailRepository;
import com.carddemo.repository.AuthorizationSummaryRepository;
import com.carddemo.repository.CardAccountXrefRepository;
import com.carddemo.repository.CardRepository;
import com.carddemo.repository.FraudRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Authorization service - migrated from Phase 5b Credit Card Authorizations:
 *   COPAUA0C (CP00 - MQ-triggered authorization processing)
 *   COPAUS0C (CPVS - Authorization Summary view)
 *   COPAUS1C (CPVD - Authorization Details view, IMS update, DB2 fraud insert)
 *   COPAUS2C (called program - fraud marking via FraudService)
 *   CBPAUP0C (batch - Purge Expired Authorizations)
 *
 * The original two-phase commit across IMS DB + DB2 is replaced with
 * a single @Transactional since both data stores are now on the same relational DB.
 */
@Service
public class AuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationService.class);

    private final AuthorizationSummaryRepository summaryRepository;
    private final AuthorizationDetailRepository detailRepository;
    private final FraudRecordRepository fraudRepository;
    private final CardRepository cardRepository;
    private final CardAccountXrefRepository xrefRepository;
    private final AccountRepository accountRepository;

    public AuthorizationService(AuthorizationSummaryRepository summaryRepository,
                                AuthorizationDetailRepository detailRepository,
                                FraudRecordRepository fraudRepository,
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

    /**
     * Process an authorization request from MQ - migrated from COPAUA0C.
     * Validates card, checks credit limit, approves or declines, updates summary.
     */
    @Transactional
    public AuthorizationDetail processAuthorization(AuthorizationRequest request) {
        log.info("Processing authorization for card: {}", request.cardNum());

        // Validate card exists and is active
        Card card = cardRepository.findById(request.cardNum())
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + request.cardNum()));

        if (!"Y".equalsIgnoreCase(card.getActiveStatus())) {
            return createDeclinedAuth(request, "INAC", "Card not active");
        }

        // Check card expiry
        if (request.cardExpiryDate() != null && !request.cardExpiryDate().isBlank()) {
            String cardExpiry = card.getExpirationDate();
            if (cardExpiry != null && !request.cardExpiryDate().equals(cardExpiry.replace("-", "").substring(0, 4))) {
                return createDeclinedAuth(request, "EXPD", "Card expiry mismatch");
            }
        }

        // Find account via xref
        List<CardAccountXref> xrefs = xrefRepository.findByCardNum(request.cardNum());
        if (xrefs.isEmpty()) {
            return createDeclinedAuth(request, "NREF", "No account reference found");
        }
        Long acctId = xrefs.get(0).getAcctId();

        // Get or create authorization summary
        AuthorizationSummary summary = summaryRepository.findByAcctId(acctId)
                .orElseGet(() -> {
                    AuthorizationSummary newSummary = new AuthorizationSummary();
                    newSummary.setAcctId(acctId);
                    newSummary.setCustId(xrefs.get(0).getCustId());
                    newSummary.setAuthStatus("A");
                    newSummary.setApprovedAuthCnt(0);
                    newSummary.setDeclinedAuthCnt(0);
                    newSummary.setApprovedAuthAmt(BigDecimal.ZERO);
                    newSummary.setDeclinedAuthAmt(BigDecimal.ZERO);
                    return summaryRepository.save(newSummary);
                });

        // Check credit limit
        Account account = accountRepository.findById(acctId).orElse(null);
        if (account != null && account.getCreditLimit() != null) {
            BigDecimal availableCredit = account.getCreditLimit()
                    .subtract(account.getCurrentBalance() != null ? account.getCurrentBalance() : BigDecimal.ZERO);
            if (request.transactionAmt().compareTo(availableCredit) > 0) {
                return createDeclinedAuth(request, summary, "CRLT", "Credit limit exceeded");
            }
        }

        // Approve authorization
        return createApprovedAuth(request, summary);
    }

    /**
     * Get authorization summary for an account - migrated from COPAUS0C.
     */
    public Optional<AuthorizationSummary> getSummary(Long acctId) {
        return summaryRepository.findByAcctId(acctId);
    }

    /**
     * Get authorization details - migrated from COPAUS1C.
     */
    public Page<AuthorizationDetail> getDetails(Long summaryId, Pageable pageable) {
        return detailRepository.findBySummaryId(summaryId, pageable);
    }

    /**
     * Mark an authorization as fraud - migrated from COPAUS2C.
     * Writes to fraud analytics table via JPA.
     */
    @Transactional
    public void markAsFraud(Long detailId) {
        AuthorizationDetail detail = detailRepository.findById(detailId)
                .orElseThrow(() -> new IllegalArgumentException("Authorization detail not found"));

        detail.setAuthFraud("F");
        detail.setFraudRptDate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        detailRepository.save(detail);

        // Create fraud record in fraud analytics table
        FraudRecord fraudRecord = new FraudRecord();
        fraudRecord.setCardNum(detail.getCardNum());
        fraudRecord.setAuthTs(Timestamp.valueOf(LocalDateTime.now()));
        fraudRecord.setAuthType(detail.getAuthType());
        fraudRecord.setTransactionAmt(detail.getTransactionAmt());
        fraudRecord.setMerchantId(detail.getMerchantId());
        fraudRecord.setMerchantName(detail.getMerchantName());
        fraudRecord.setAuthFraud("F");
        fraudRecord.setFraudRptDate(LocalDate.now());
        fraudRepository.save(fraudRecord);
    }

    /**
     * Purge expired authorizations - migrated from CBPAUP0C batch.
     * Deletes unmatched pending authorizations that have expired,
     * and adjusts available credit when doing so.
     */
    @Transactional
    public int purgeExpiredAuthorizations(int expiryDays) {
        List<AuthorizationDetail> pendingDetails = detailRepository.findByMatchStatus("P");
        int purgedCount = 0;
        String cutoffDate = LocalDate.now().minusDays(expiryDays)
                .format(DateTimeFormatter.ofPattern("yyMMdd"));

        for (AuthorizationDetail detail : pendingDetails) {
            if (detail.getAuthOrigDate() != null && detail.getAuthOrigDate().compareTo(cutoffDate) < 0) {
                detail.setMatchStatus("E");
                detailRepository.save(detail);

                // Adjust available credit on the account
                if (detail.getSummaryId() != null && detail.getApprovedAmt() != null) {
                    summaryRepository.findById(detail.getSummaryId()).ifPresent(summary -> {
                        if (summary.getCreditBalance() != null) {
                            summary.setCreditBalance(
                                    summary.getCreditBalance().subtract(detail.getApprovedAmt()));
                            summaryRepository.save(summary);
                        }
                    });
                }
                purgedCount++;
            }
        }

        log.info("Purged {} expired authorizations", purgedCount);
        return purgedCount;
    }

    private AuthorizationDetail createApprovedAuth(AuthorizationRequest request, AuthorizationSummary summary) {
        AuthorizationDetail detail = buildDetail(request, summary.getId());
        detail.setAuthRespCode("00");
        detail.setApprovedAmt(request.transactionAmt());
        detail.setMatchStatus("P");
        detail = detailRepository.save(detail);

        summary.setApprovedAuthCnt(summary.getApprovedAuthCnt() + 1);
        BigDecimal approvedAmt = summary.getApprovedAuthAmt() != null
                ? summary.getApprovedAuthAmt() : BigDecimal.ZERO;
        summary.setApprovedAuthAmt(approvedAmt.add(request.transactionAmt()));
        summaryRepository.save(summary);

        return detail;
    }

    private AuthorizationDetail createDeclinedAuth(AuthorizationRequest request, String respCode, String reason) {
        return createDeclinedAuth(request, null, respCode, reason);
    }

    private AuthorizationDetail createDeclinedAuth(AuthorizationRequest request,
                                                   AuthorizationSummary summary,
                                                   String respCode, String reason) {
        AuthorizationDetail detail = buildDetail(request, summary != null ? summary.getId() : null);
        detail.setAuthRespCode(respCode.substring(0, Math.min(2, respCode.length())));
        detail.setAuthRespReason(reason.substring(0, Math.min(4, reason.length())));
        detail.setApprovedAmt(BigDecimal.ZERO);
        detail.setMatchStatus("D");
        detail = detailRepository.save(detail);

        if (summary != null) {
            summary.setDeclinedAuthCnt(summary.getDeclinedAuthCnt() + 1);
            BigDecimal declinedAmt = summary.getDeclinedAuthAmt() != null
                    ? summary.getDeclinedAuthAmt() : BigDecimal.ZERO;
            summary.setDeclinedAuthAmt(declinedAmt.add(request.transactionAmt()));
            summaryRepository.save(summary);
        }

        return detail;
    }

    private AuthorizationDetail buildDetail(AuthorizationRequest request, Long summaryId) {
        AuthorizationDetail detail = new AuthorizationDetail();
        detail.setSummaryId(summaryId);
        detail.setAuthDate(request.authDate());
        detail.setAuthTime(request.authTime());
        detail.setAuthOrigDate(request.authDate());
        detail.setAuthOrigTime(request.authTime());
        detail.setCardNum(request.cardNum());
        detail.setAuthType(request.authType());
        detail.setCardExpiryDate(request.cardExpiryDate());
        detail.setMessageType(request.messageType());
        detail.setMessageSource(request.messageSource());
        detail.setProcessingCode(Integer.parseInt(request.processingCode()));
        detail.setTransactionAmt(request.transactionAmt());
        detail.setMerchantCategoryCode(request.merchantCategoryCode());
        detail.setAcqrCountryCode(request.acqrCountryCode());
        detail.setPosEntryMode(request.posEntryMode());
        detail.setMerchantId(request.merchantId());
        detail.setMerchantName(request.merchantName());
        detail.setMerchantCity(request.merchantCity());
        detail.setMerchantState(request.merchantState());
        detail.setMerchantZip(request.merchantZip());
        detail.setTransactionId(request.transactionId());
        return detail;
    }
}
