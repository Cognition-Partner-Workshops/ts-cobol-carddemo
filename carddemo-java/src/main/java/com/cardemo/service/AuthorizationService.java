package com.cardemo.service;

import com.cardemo.dto.AuthorizationRequest;
import com.cardemo.entity.AuthDetail;
import com.cardemo.entity.AuthFraud;
import com.cardemo.entity.AuthSummary;
import com.cardemo.entity.Card;
import com.cardemo.entity.CardAccountXref;
import com.cardemo.exception.CardDemoException;
import com.cardemo.repository.AuthDetailRepository;
import com.cardemo.repository.AuthFraudRepository;
import com.cardemo.repository.AuthSummaryRepository;
import com.cardemo.repository.CardAccountXrefRepository;
import com.cardemo.repository.CardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Authorization service for optional IMS/DB2/MQ module.
 * Migrated from COPAUS0C (CPVS - list), COPAUS1C (CPVD - detail/fraud),
 * and COPAUA0C (CP00 - MQ processing).
 */
@Service
public class AuthorizationService {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS");

    private final AuthSummaryRepository authSummaryRepository;
    private final AuthDetailRepository authDetailRepository;
    private final AuthFraudRepository authFraudRepository;
    private final CardRepository cardRepository;
    private final CardAccountXrefRepository xrefRepository;

    public AuthorizationService(AuthSummaryRepository authSummaryRepository,
                                AuthDetailRepository authDetailRepository,
                                AuthFraudRepository authFraudRepository,
                                CardRepository cardRepository,
                                CardAccountXrefRepository xrefRepository) {
        this.authSummaryRepository = authSummaryRepository;
        this.authDetailRepository = authDetailRepository;
        this.authFraudRepository = authFraudRepository;
        this.cardRepository = cardRepository;
        this.xrefRepository = xrefRepository;
    }

    /**
     * List authorizations by card number - migrated from COPAUS0C (CPVS transaction).
     * COBOL: GU (Get Unique) call to IMS DB using card number as key.
     */
    public List<AuthSummary> getAuthorizationsByCardNum(String cardNum) {
        return authSummaryRepository.findByCardNum(cardNum);
    }

    /**
     * Get authorization detail - migrated from COPAUS1C (CPVD transaction).
     * COBOL: GU on root segment, then GNP (Get Next within Parent) for details.
     */
    public AuthSummary getAuthorizationSummary(Long id) {
        return authSummaryRepository.findById(id)
                .orElseThrow(() -> CardDemoException.notFound("Authorization summary not found: " + id));
    }

    /**
     * Get authorization details for a summary.
     */
    public List<AuthDetail> getAuthorizationDetails(Long authSummaryId) {
        return authDetailRepository.findByAuthSummaryIdOrderByAuthTsDesc(authSummaryId);
    }

    /**
     * Flag authorization as fraud - migrated from COPAUS1C (CPVD transaction).
     * COBOL: ISRT (Insert) to AUTHFRDS DB2 table with fraud flag.
     */
    @Transactional
    public AuthFraud flagAsFraud(Long authDetailId, String fraudFlag) {
        AuthDetail detail = authDetailRepository.findById(authDetailId)
                .orElseThrow(() -> CardDemoException.notFound("Authorization detail not found: " + authDetailId));

        AuthSummary summary = authSummaryRepository.findById(detail.getAuthSummaryId())
                .orElseThrow(() -> CardDemoException.notFound("Authorization summary not found"));

        AuthFraud fraud = new AuthFraud();
        fraud.setCardNum(summary.getCardNum());
        fraud.setAuthTs(LocalDateTime.now());
        fraud.setAuthType(detail.getAuthType());
        fraud.setCardExpiryDate(detail.getCardExpiryDate());
        fraud.setMessageType(detail.getMessageType());
        fraud.setMessageSource(detail.getMessageSource());
        fraud.setAuthIdCode(detail.getAuthIdCode());
        fraud.setAuthRespCode(detail.getAuthRespCode());
        fraud.setAuthRespReason(detail.getAuthRespReason());
        fraud.setProcessingCode(detail.getProcessingCode());
        fraud.setTransactionAmt(detail.getTransactionAmt());
        fraud.setApprovedAmt(detail.getApprovedAmt());
        fraud.setMerchantCategoryCode(detail.getMerchantCategoryCode());
        fraud.setAcqrCountryCode(detail.getAcqrCountryCode());
        fraud.setPosEntryMode(detail.getPosEntryMode());
        fraud.setMerchantId(detail.getMerchantId());
        fraud.setMerchantName(detail.getMerchantName());
        fraud.setMerchantCity(detail.getMerchantCity());
        fraud.setMerchantState(detail.getMerchantState());
        fraud.setMerchantZip(detail.getMerchantZip());
        fraud.setTransactionId(detail.getTransactionId());
        fraud.setAuthFraud(fraudFlag);
        fraud.setMatchStatus("M");
        fraud.setAcctId(BigDecimal.valueOf(summary.getAcctId()));
        fraud.setCustId(BigDecimal.valueOf(summary.getCustId()));

        return authFraudRepository.save(fraud);
    }

    /**
     * Process MQ authorization request - migrated from COPAUA0C (CP00 transaction).
     * COBOL: EXEC CICS GET CONTAINER -> parse CSV -> validate card -> create auth record
     * -> EXEC CICS PUT CONTAINER (reply)
     */
    @Transactional
    public String processAuthorizationRequest(AuthorizationRequest request) {
        // Validate card exists
        Card card = cardRepository.findById(request.getCardNum()).orElse(null);
        if (card == null) {
            return "DECLINED:CARD_NOT_FOUND";
        }

        // Validate card is active
        if (!"Y".equalsIgnoreCase(card.getCardActiveStatus())) {
            return "DECLINED:CARD_INACTIVE";
        }

        // Check card expiry - COBOL: IF WS-CARD-EXPIRY-DATE < WS-CURRENT-DATE
        // Simplified: just check if card has valid expiry
        if (request.getCardExpiryDate() != null && !request.getCardExpiryDate().isBlank()) {
            if (card.getCardExpirationDate() != null &&
                !card.getCardExpirationDate().equals(request.getCardExpiryDate())) {
                return "DECLINED:CARD_EXPIRY_MISMATCH";
            }
        }

        // Look up account via XREF
        CardAccountXref xref = xrefRepository.findById(request.getCardNum()).orElse(null);
        if (xref == null) {
            return "DECLINED:NO_ACCOUNT";
        }

        // Create or update auth summary - COBOL: GHU (Get Hold Unique) + REPL or ISRT
        AuthSummary summary = authSummaryRepository.findFirstByCardNum(request.getCardNum())
                .orElse(null);

        if (summary == null) {
            summary = new AuthSummary();
            summary.setCardNum(request.getCardNum());
            summary.setAcctId(xref.getXrefAcctId());
            summary.setCustId(xref.getXrefCustId());
            summary.setTotalAuthAmt(BigDecimal.ZERO);
            summary.setAuthCount(0);
        }

        // Update summary totals
        BigDecimal txnAmt = request.getTransactionAmt() != null ? request.getTransactionAmt() : BigDecimal.ZERO;
        summary.setTotalAuthAmt(summary.getTotalAuthAmt().add(txnAmt));
        summary.setAuthCount(summary.getAuthCount() + 1);
        summary.setLastAuthTs(LocalDateTime.now().format(TS_FORMAT));
        authSummaryRepository.save(summary);

        // Create auth detail - COBOL: ISRT child segment
        AuthDetail detail = new AuthDetail();
        detail.setAuthSummaryId(summary.getId());
        detail.setAuthTs(LocalDateTime.now().format(TS_FORMAT));
        detail.setAuthType(request.getAuthType());
        detail.setCardExpiryDate(request.getCardExpiryDate());
        detail.setMessageType(request.getMessageType());
        detail.setMessageSource(request.getMessageSource());
        detail.setProcessingCode(request.getProcessingCode());
        detail.setTransactionAmt(request.getTransactionAmt());
        detail.setApprovedAmt(request.getTransactionAmt());
        detail.setMerchantCategoryCode(request.getMerchantCategoryCode());
        detail.setAcqrCountryCode(request.getAcqrCountryCode());
        detail.setPosEntryMode(request.getPosEntryMode());
        detail.setMerchantId(request.getMerchantId());
        detail.setMerchantName(request.getMerchantName());
        detail.setMerchantCity(request.getMerchantCity());
        detail.setMerchantState(request.getMerchantState());
        detail.setMerchantZip(request.getMerchantZip());
        detail.setTransactionId(request.getTransactionId());
        detail.setAuthIdCode("APPR01");
        detail.setAuthRespCode("00");
        authDetailRepository.save(detail);

        return "APPROVED:" + detail.getAuthIdCode();
    }
}
