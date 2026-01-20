package com.carddemo.authorization.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorizationResponseDto {
    private String authId;
    private String status;
    private String responseCode;
    private String declineReason;
    private BigDecimal authorizedAmount;
    private BigDecimal availableCredit;
    private LocalDateTime responseTimestamp;
}
