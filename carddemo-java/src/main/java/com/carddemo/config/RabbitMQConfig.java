package com.carddemo.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "true", matchIfMissing = false)
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.auth.request}")
    private String authRequestQueue;

    @Value("${rabbitmq.queue.auth.reply}")
    private String authReplyQueue;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing.key.auth}")
    private String routingKey;

    @Bean
    public Queue authRequestQueue() {
        return new Queue(authRequestQueue, true);
    }

    @Bean
    public Queue authReplyQueue() {
        return new Queue(authReplyQueue, true);
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(exchange);
    }

    @Bean
    public Binding authBinding(Queue authRequestQueue, DirectExchange exchange) {
        return BindingBuilder.bind(authRequestQueue).to(exchange).with(routingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
