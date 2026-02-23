package com.carddemo.integration.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.jms.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

/**
 * JMS/ActiveMQ Artemis configuration replacing IBM MQ integration.
 *
 * IBM MQ → ActiveMQ Artemis mapping:
 *   CSQPUT1 (MQ PUT)    → JmsTemplate.convertAndSend()
 *   CSQGET1 (MQ GET)    → @JmsListener annotated methods
 *   MQ Queue Manager    → ActiveMQ Artemis broker
 *   MQ Queue CARDDEMO.Q → ActiveMQ destination "carddemo.transactions"
 *
 * COBOL programs replaced:
 *   CODATE01 (CDRD) → Date request/response via JMS
 *   COACCT01 (CDRA) → Account data request/response via JMS
 */
@Configuration
@EnableJms
public class JmsConfig {

    public static final String TRANSACTION_QUEUE = "carddemo.transactions";
    public static final String AUTHORIZATION_QUEUE = "carddemo.authorizations";
    public static final String NOTIFICATION_QUEUE = "carddemo.notifications";

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrency("1-5");
        factory.setErrorHandler(t -> {
            // TODO Phase 3: Implement error handling (dead letter queue, retry)
        });
        return factory;
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory,
                                   MessageConverter messageConverter) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setDefaultDestinationName(TRANSACTION_QUEUE);
        return template;
    }

    @Bean
    public MessageConverter messageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        converter.setObjectMapper(mapper);
        return converter;
    }
}
