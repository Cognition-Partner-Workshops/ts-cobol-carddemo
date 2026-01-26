package com.aws.carddemo.messaging;

import com.aws.carddemo.dto.AuthRequestDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationMessageProducer.class);

    public static final String EXCHANGE_NAME = "carddemo.authorization";
    public static final String ROUTING_KEY_REQUEST = "authorization.request";
    public static final String ROUTING_KEY_RESPONSE = "authorization.response";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public AuthorizationMessageProducer(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendAuthorizationRequest(AuthRequestDto request) {
        try {
            String message = objectMapper.writeValueAsString(request);
            rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY_REQUEST, message);
            log.info("Sent authorization request for card: {}", maskCardNumber(request.getCardNum()));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize authorization request", e);
            throw new RuntimeException("Failed to send authorization request", e);
        }
    }

    public void sendAuthorizationResponse(AuthRequestDto response) {
        try {
            String message = objectMapper.writeValueAsString(response);
            rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY_RESPONSE, message);
            log.info("Sent authorization response for card: {} with status: {}", 
                    maskCardNumber(response.getCardNum()), response.getAuthStatus());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize authorization response", e);
            throw new RuntimeException("Failed to send authorization response", e);
        }
    }

    private String maskCardNumber(String cardNum) {
        if (cardNum == null || cardNum.length() < 4) {
            return "****";
        }
        return "****" + cardNum.substring(cardNum.length() - 4);
    }
}
