package com.aws.carddemo.messaging;

import com.aws.carddemo.dto.AccountDto;
import com.aws.carddemo.service.AccountService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AccountExtractionMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(AccountExtractionMessageConsumer.class);

    private final AccountService accountService;
    private final AccountExtractionMessageProducer messageProducer;
    private final ObjectMapper objectMapper;

    public AccountExtractionMessageConsumer(AccountService accountService,
                                             AccountExtractionMessageProducer messageProducer,
                                             ObjectMapper objectMapper) {
        this.accountService = accountService;
        this.messageProducer = messageProducer;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "carddemo.account.extract.request.queue")
    public void processExtractionRequest(String message) {
        log.info("Received account extraction request");
        
        try {
            JsonNode node = objectMapper.readTree(message);
            String requestId = node.get("requestId").asText();
            String criteria = node.has("criteria") ? node.get("criteria").asText() : "ALL";
            
            List<AccountDto> accounts;
            
            if ("ACTIVE".equalsIgnoreCase(criteria)) {
                Page<AccountDto> page = accountService.getActiveAccounts(PageRequest.of(0, 1000));
                accounts = page.getContent();
            } else if ("OVERLIMIT".equalsIgnoreCase(criteria)) {
                accounts = accountService.getOverlimitAccounts();
            } else if ("EXPIRED".equalsIgnoreCase(criteria)) {
                accounts = accountService.getExpiredAccounts();
            } else {
                Page<AccountDto> page = accountService.getAllAccounts(PageRequest.of(0, 1000));
                accounts = page.getContent();
            }
            
            messageProducer.sendExtractionResponse(requestId, accounts);
            log.info("Processed extraction request: {} with {} accounts", requestId, accounts.size());
            
        } catch (Exception e) {
            log.error("Failed to process account extraction request", e);
        }
    }
}
