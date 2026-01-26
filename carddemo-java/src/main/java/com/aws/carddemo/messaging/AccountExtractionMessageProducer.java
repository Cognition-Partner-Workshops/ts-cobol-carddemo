package com.aws.carddemo.messaging;

import com.aws.carddemo.dto.AccountDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AccountExtractionMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(AccountExtractionMessageProducer.class);

    public static final String EXCHANGE_NAME = "carddemo.account";
    public static final String ROUTING_KEY_EXTRACT_REQUEST = "account.extract.request";
    public static final String ROUTING_KEY_EXTRACT_RESPONSE = "account.extract.response";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public AccountExtractionMessageProducer(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendExtractionRequest(String requestId, String criteria) {
        String message = String.format("{\"requestId\":\"%s\",\"criteria\":\"%s\"}", requestId, criteria);
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY_EXTRACT_REQUEST, message);
        log.info("Sent account extraction request: {}", requestId);
    }

    public void sendExtractionResponse(String requestId, List<AccountDto> accounts) {
        try {
            String accountsJson = objectMapper.writeValueAsString(accounts);
            String message = String.format("{\"requestId\":\"%s\",\"accounts\":%s}", requestId, accountsJson);
            rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY_EXTRACT_RESPONSE, message);
            log.info("Sent account extraction response for request: {} with {} accounts", requestId, accounts.size());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize account extraction response", e);
            throw new RuntimeException("Failed to send account extraction response", e);
        }
    }
}
