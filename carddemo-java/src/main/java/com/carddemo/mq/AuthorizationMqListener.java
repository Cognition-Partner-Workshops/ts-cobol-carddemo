package com.carddemo.mq;

import com.carddemo.dto.AuthorizationRequest;
import com.carddemo.entity.AuthorizationDetail;
import com.carddemo.service.AuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * MQ Listener for credit card authorization requests - migrated from Phase 5b COPAUA0C (CP00).
 * Original COBOL COPAUA0C: CICS transaction triggered by MQ message arrival.
 * Reads CSV-formatted authorization request from MQ queue, processes it,
 * and writes result to IMS DB + DB2 (now single relational DB with @Transactional).
 *
 * MQ Authorization Request Message Format (CSV):
 * AUTH-DATE, AUTH-TIME, CARD-NUM, AUTH-TYPE, CARD-EXPIRY-DATE,
 * MESSAGE-TYPE, MESSAGE-SOURCE, PROCESSING-CODE, TRANSACTION-AMT,
 * MERCHANT-CATEGORY-CODE, ACQR-COUNTRY-CODE, POS-ENTRY-MODE,
 * MERCHANT-ID, MERCHANT-NAME, MERCHANT-CITY, MERCHANT-STATE,
 * MERCHANT-ZIP, TRANSACTION-ID
 */
@Component
public class AuthorizationMqListener {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationMqListener.class);

    private final AuthorizationService authorizationService;

    public AuthorizationMqListener(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    /**
     * Process authorization request from MQ queue.
     * Replaces COPAUA0C CICS MQ trigger.
     */
    @JmsListener(destination = "${carddemo.mq.auth-queue:DEV.QUEUE.AUTH}",
                 containerFactory = "jmsListenerContainerFactory")
    public void onAuthorizationRequest(String message) {
        log.info("Received authorization request from MQ");
        try {
            AuthorizationRequest request = AuthorizationRequest.fromCsv(message);
            AuthorizationDetail result = authorizationService.processAuthorization(request);
            log.info("Authorization processed. Card: {}, Response: {}, Match: {}",
                    result.getCardNum(), result.getAuthRespCode(), result.getMatchStatus());
        } catch (Exception e) {
            log.error("Error processing authorization request: {}", e.getMessage(), e);
        }
    }
}
