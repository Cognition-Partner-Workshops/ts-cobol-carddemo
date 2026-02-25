package com.cardemo.jms;

import com.cardemo.dto.AuthorizationRequest;
import com.cardemo.service.AuthorizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * JMS Message Listener for authorization processing (Optional MQ module).
 * Migrated from COPAUA0C (CP00 transaction) - MQ trigger program.
 *
 * COBOL flow:
 * 1. EXEC CICS GET CONTAINER(request) -> parse CSV message from input queue
 * 2. Validate card number, check expiry, verify account
 * 3. Create authorization record in IMS DB
 * 4. EXEC CICS PUT CONTAINER(reply) -> send approval/decline to reply queue
 *
 * MQ Queues:
 * - Input:  AWS.M2.CARDDEMO.PAUTH.REQUEST
 * - Output: AWS.M2.CARDDEMO.PAUTH.REPLY
 */
@Component
public class AuthorizationMessageListener {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationMessageListener.class);

    private final AuthorizationService authorizationService;
    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;

    public AuthorizationMessageListener(AuthorizationService authorizationService,
                                        JmsTemplate jmsTemplate,
                                        ObjectMapper objectMapper) {
        this.authorizationService = authorizationService;
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Listen for authorization requests on the input queue.
     * Migrated from COPAUA0C which is triggered by MQ message arrival.
     *
     * COBOL message format (CSV):
     * AUTH-DATE,AUTH-TIME,CARD-NUM,AUTH-TYPE,CARD-EXPIRY,MSG-TYPE,MSG-SOURCE,
     * PROC-CODE,TXN-AMT,MERCH-CAT-CODE,ACQR-COUNTRY,POS-ENTRY,MERCH-ID,
     * MERCH-NAME,MERCH-CITY,MERCH-STATE,MERCH-ZIP,TXN-ID
     *
     * Java migration: accepts JSON-formatted AuthorizationRequest.
     */
    @JmsListener(destination = "${jms.queues.auth-request:AWS.M2.CARDDEMO.PAUTH.REQUEST}")
    public void onAuthorizationRequest(String message) {
        log.info("CP00: Received authorization request from MQ");

        try {
            AuthorizationRequest request = objectMapper.readValue(message, AuthorizationRequest.class);

            // Process authorization - delegates to AuthorizationService
            String result = authorizationService.processAuthorizationRequest(request);

            log.info("CP00: Authorization result for card {}: {}", request.getCardNum(), result);

            // Send reply to output queue
            // COBOL: EXEC CICS PUT CONTAINER(WS-REPLY-CHANNEL) FROM(WS-REPLY-MSG)
            jmsTemplate.convertAndSend("AWS.M2.CARDDEMO.PAUTH.REPLY", result);

        } catch (Exception e) {
            log.error("CP00: Error processing authorization request", e);
            jmsTemplate.convertAndSend("AWS.M2.CARDDEMO.PAUTH.REPLY",
                    "DECLINED:PROCESSING_ERROR:" + e.getMessage());
        }
    }
}
