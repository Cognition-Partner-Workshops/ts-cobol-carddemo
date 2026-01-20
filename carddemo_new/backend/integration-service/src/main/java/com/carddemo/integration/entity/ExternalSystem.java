package com.carddemo.integration.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "external_systems")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExternalSystem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "system_code", length = 20, unique = true)
    private String systemCode;

    @Column(name = "system_name", length = 100)
    private String systemName;

    @Column(name = "system_type", length = 30)
    private String systemType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "endpoint_url", length = 500)
    private String endpointUrl;

    @Column(name = "auth_type", length = 20)
    private String authType;

    @Column(name = "api_key", length = 200)
    private String apiKey;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "password", length = 200)
    private String password;

    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "last_health_check")
    private LocalDateTime lastHealthCheck;

    @Column(name = "health_status", length = 20)
    private String healthStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
