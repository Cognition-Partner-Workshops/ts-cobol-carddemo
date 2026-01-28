package com.carddemo.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret = "carddemo-secret-key-for-jwt-token-generation-must-be-at-least-256-bits";
    private long accessTokenExpiration = 3600000;
    private long refreshTokenExpiration = 86400000;
}
