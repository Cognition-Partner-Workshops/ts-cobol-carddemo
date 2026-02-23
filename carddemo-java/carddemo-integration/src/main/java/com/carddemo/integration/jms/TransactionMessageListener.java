package com.carddemo.integration.jms;

import com.carddemo.integration.config.JmsConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * JMS message listener for transaction events.
 * Replaces IBM MQ CSQGET1 calls in COBOL programs.
 *
 * COBOL → Java mapping:
 *   EXEC CICS LINK PROGRAM('CSQGET1') COMMAREA(msg-data)
 *   → @JmsListener method receives messages asynchronously
 */
@Component
@Slf4j
public class TransactionMessageListener {

    /**
     * Listen for transaction events from the transaction queue.
     * TODO Phase 3: Implement actual business logic for each event type.
     */
    @JmsListener(destination = JmsConfig.TRANSACTION_QUEUE,
            containerFactory = "jmsListenerContainerFactory")
    public void onTransactionEvent(Map<String, String> message) {
        String transactionId = message.get("transactionId");
        String eventType = message.get("eventType");
        log.info("Received transaction event: {} for transaction: {}", eventType, transactionId);

        // TODO Phase 3: Route to appropriate handler based on event type
        // switch (eventType) {
        //     case "CREATED" -> handleTransactionCreated(transactionId);
        //     case "POSTED" -> handleTransactionPosted(transactionId);
        //     case "REVERSED" -> handleTransactionReversed(transactionId);
        // }
    }

    /**
     * Listen for authorization responses from the authorization queue.
     * TODO Phase 3: Implement authorization response handling.
     */
    @JmsListener(destination = JmsConfig.AUTHORIZATION_QUEUE,
            containerFactory = "jmsListenerContainerFactory")
    public void onAuthorizationResponse(Map<String, String> message) {
        String transactionId = message.get("transactionId");
        log.info("Received authorization response for transaction: {}", transactionId);

        // TODO Phase 3: Process authorization response
        // - Update transaction status
        // - Notify originating system
    }
}
