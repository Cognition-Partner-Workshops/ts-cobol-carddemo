package com.aws.carddemo.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String AUTHORIZATION_EXCHANGE = "carddemo.authorization";
    public static final String AUTHORIZATION_REQUEST_QUEUE = "carddemo.authorization.request.queue";
    public static final String AUTHORIZATION_RESPONSE_QUEUE = "carddemo.authorization.response.queue";
    public static final String AUTHORIZATION_REQUEST_ROUTING_KEY = "authorization.request";
    public static final String AUTHORIZATION_RESPONSE_ROUTING_KEY = "authorization.response";

    public static final String ACCOUNT_EXCHANGE = "carddemo.account";
    public static final String ACCOUNT_EXTRACT_REQUEST_QUEUE = "carddemo.account.extract.request.queue";
    public static final String ACCOUNT_EXTRACT_RESPONSE_QUEUE = "carddemo.account.extract.response.queue";
    public static final String ACCOUNT_EXTRACT_REQUEST_ROUTING_KEY = "account.extract.request";
    public static final String ACCOUNT_EXTRACT_RESPONSE_ROUTING_KEY = "account.extract.response";

    @Bean
    public TopicExchange authorizationExchange() {
        return new TopicExchange(AUTHORIZATION_EXCHANGE);
    }

    @Bean
    public Queue authorizationRequestQueue() {
        return QueueBuilder.durable(AUTHORIZATION_REQUEST_QUEUE).build();
    }

    @Bean
    public Queue authorizationResponseQueue() {
        return QueueBuilder.durable(AUTHORIZATION_RESPONSE_QUEUE).build();
    }

    @Bean
    public Binding authorizationRequestBinding(Queue authorizationRequestQueue, TopicExchange authorizationExchange) {
        return BindingBuilder.bind(authorizationRequestQueue)
                .to(authorizationExchange)
                .with(AUTHORIZATION_REQUEST_ROUTING_KEY);
    }

    @Bean
    public Binding authorizationResponseBinding(Queue authorizationResponseQueue, TopicExchange authorizationExchange) {
        return BindingBuilder.bind(authorizationResponseQueue)
                .to(authorizationExchange)
                .with(AUTHORIZATION_RESPONSE_ROUTING_KEY);
    }

    @Bean
    public TopicExchange accountExchange() {
        return new TopicExchange(ACCOUNT_EXCHANGE);
    }

    @Bean
    public Queue accountExtractRequestQueue() {
        return QueueBuilder.durable(ACCOUNT_EXTRACT_REQUEST_QUEUE).build();
    }

    @Bean
    public Queue accountExtractResponseQueue() {
        return QueueBuilder.durable(ACCOUNT_EXTRACT_RESPONSE_QUEUE).build();
    }

    @Bean
    public Binding accountExtractRequestBinding(Queue accountExtractRequestQueue, TopicExchange accountExchange) {
        return BindingBuilder.bind(accountExtractRequestQueue)
                .to(accountExchange)
                .with(ACCOUNT_EXTRACT_REQUEST_ROUTING_KEY);
    }

    @Bean
    public Binding accountExtractResponseBinding(Queue accountExtractResponseQueue, TopicExchange accountExchange) {
        return BindingBuilder.bind(accountExtractResponseQueue)
                .to(accountExchange)
                .with(ACCOUNT_EXTRACT_RESPONSE_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
