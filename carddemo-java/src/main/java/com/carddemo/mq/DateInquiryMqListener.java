package com.carddemo.mq;

import com.carddemo.dto.DateRequest;
import com.carddemo.dto.DateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * MQ Listener for date inquiry messages - migrated from Phase 5c CODATE01 (CDRD).
 * Original COBOL CODATE01: CICS transaction that responds to date inquiry MQ messages.
 * Reads REQUEST-TYPE='DATE' + REQUEST-ID from MQ, responds with system date.
 */
@Component
public class DateInquiryMqListener {

    private static final Logger log = LoggerFactory.getLogger(DateInquiryMqListener.class);

    private final JmsTemplate jmsTemplate;

    public DateInquiryMqListener(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @JmsListener(destination = "${carddemo.mq.date-request-queue:DEV.QUEUE.DATE.REQ}",
                 containerFactory = "jmsListenerContainerFactory")
    public void onDateRequest(String message) {
        log.info("Received date inquiry request from MQ");
        try {
            DateRequest request = DateRequest.parse(message);

            if (!"DATE".equals(request.requestType())) {
                log.warn("Invalid request type: {}", request.requestType());
                return;
            }

            String systemDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            DateResponse response = new DateResponse("DATE", request.requestId(), systemDate);

            jmsTemplate.convertAndSend("DEV.QUEUE.DATE.RESP", response.toMessage());
            log.info("Sent date response: {} for request ID: {}", systemDate, request.requestId());
        } catch (Exception e) {
            log.error("Error processing date inquiry: {}", e.getMessage(), e);
        }
    }
}
