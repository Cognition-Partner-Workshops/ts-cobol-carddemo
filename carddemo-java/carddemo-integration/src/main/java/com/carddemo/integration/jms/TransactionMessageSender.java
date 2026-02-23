package com.carddemo.integration.jms;

import com.carddemo.integration.config.JmsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * JMS message sender for transaction events.
 * Replaces IBM MQ CSQPUT1 calls in COBOL programs.
 *
 * COBOL → Java mapping:
 *   EXEC CICS LINK PROGRAM('CSQPUT1') COMMAREA(msg-data)
 *   → transactionMessageSender.sendTransactionEvent(payload)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionMessageSender {

    private final JmsTemplate jmsTemplate;

    /**
     * Send a transaction event to the transaction queue.
     *
     * @param transactionId the transaction identifier
     * @param eventType     the event type (e.g., "CREATED", "POSTED", "REVERSED")
     */
    public void sendTransactionEvent(String transactionId, String eventType) {
        Map<String, String> payload = Map.of(
                "transactionId", transactionId,
                "eventType", eventType
        );
        jmsTemplate.convertAndSend(JmsConfig.TRANSACTION_QUEUE, payload);
        log.info("Sent transaction event: {} for transaction: {}", eventType, transactionId);
    }

    /**
     * Send an authorization request to the authorization queue.
     *
     * @param transactionId the transaction identifier
     * @param cardNumber    the card number
     * @param amount        the transaction amount as string
     */
    public void sendAuthorizationRequest(String transactionId, String cardNumber, String amount) {
        Map<String, String> payload = Map.of(
                "transactionId", transactionId,
                "cardNumber", cardNumber,
                "amount", amount,
                "requestType", "AUTHORIZATION"
        );
        jmsTemplate.convertAndSend(JmsConfig.AUTHORIZATION_QUEUE, payload);
        log.info("Sent authorization request for transaction: {}", transactionId);
    }
}
