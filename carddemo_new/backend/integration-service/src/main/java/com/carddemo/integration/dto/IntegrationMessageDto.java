package com.carddemo.integration.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationMessageDto {
    private String correlationId;
    private String sourceSystem;
    private String targetSystem;
    private String messageType;
    private String payload;
    private Integer maxRetries;
}
