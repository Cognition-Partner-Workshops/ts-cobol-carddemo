package com.carddemo.integration.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExternalSystemDto {
    private String systemCode;
    private String systemName;
    private String systemType;
    private String description;
    private String endpointUrl;
    private String authType;
    private String apiKey;
    private String username;
    private String password;
    private Integer timeoutSeconds;
    private Integer retryCount;
    private Boolean active;
}
