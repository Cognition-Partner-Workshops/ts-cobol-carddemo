package com.carddemo.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**")
                        .uri("http://localhost:8081"))
                .route("customer-service", r -> r
                        .path("/api/v1/customers/**")
                        .uri("http://localhost:8082"))
                .route("account-service", r -> r
                        .path("/api/v1/accounts/**")
                        .uri("http://localhost:8083"))
                .route("card-service", r -> r
                        .path("/api/v1/cards/**")
                        .uri("http://localhost:8084"))
                .route("transaction-service", r -> r
                        .path("/api/v1/transactions/**")
                        .uri("http://localhost:8085"))
                .route("payment-service", r -> r
                        .path("/api/v1/payments/**")
                        .uri("http://localhost:8086"))
                .route("reporting-service", r -> r
                        .path("/api/v1/reports/**")
                        .uri("http://localhost:8087"))
                .route("batch-service", r -> r
                        .path("/api/v1/batch/**")
                        .uri("http://localhost:8088"))
                .build();
    }
}
