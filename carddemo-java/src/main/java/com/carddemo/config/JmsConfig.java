package com.carddemo.config;

import jakarta.jms.ConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;

/**
 * JMS configuration for MQ integration.
 * Replaces IBM MQ CICS trigger definitions for CP00, CDRD, CDRA transactions.
 * When no MQ broker is available (e.g., dev/test), Spring Boot auto-config
 * provides an embedded Artemis broker.
 */
@Configuration
@EnableJms
public class JmsConfig {

    @Bean
    @ConditionalOnBean(ConnectionFactory.class)
    public JmsListenerContainerFactory<?> jmsListenerContainerFactory(ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrency("1-5");
        factory.setSessionTransacted(true);
        return factory;
    }

    @Bean
    @ConditionalOnBean(ConnectionFactory.class)
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setReceiveTimeout(5000);
        return template;
    }
}
