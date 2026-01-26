package com.aws.carddemo.messaging;

import com.aws.carddemo.dto.AuthRequestDto;
import com.aws.carddemo.service.AuthorizationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationMessageConsumer.class);

    private final AuthorizationService authorizationService;
    private final AuthorizationMessageProducer messageProducer;
    private final ObjectMapper objectMapper;

    public AuthorizationMessageConsumer(AuthorizationService authorizationService,
                                         AuthorizationMessageProducer messageProducer,
                                         ObjectMapper objectMapper) {
        this.authorizationService = authorizationService;
        this.messageProducer = messageProducer;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "carddemo.authorization.request.queue")
    public void processAuthorizationRequest(String message) {
        log.info("Received authorization request message");
        
        try {
            AuthRequestDto request = objectMapper.readValue(message, AuthRequestDto.class);
            AuthRequestDto response = authorizationService.processAuthorizationRequest(request);
            messageProducer.sendAuthorizationResponse(response);
            
            log.info("Processed authorization request, status: {}", response.getAuthStatus());
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize authorization request", e);
        } catch (Exception e) {
            log.error("Failed to process authorization request", e);
        }
    }
}
