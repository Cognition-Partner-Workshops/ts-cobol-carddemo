package com.aws.carddemo.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequestDto {

    private Long authId;

    @NotBlank(message = "Card number is required")
    @Size(max = 16, message = "Card number must be at most 16 characters")
    private String cardNum;

    @NotNull(message = "Transaction amount is required")
    @DecimalMin(value = "0.01", message = "Transaction amount must be positive")
    private BigDecimal tranAmt;

    private Long merchantId;

    @Size(max = 50, message = "Merchant name must be at most 50 characters")
    private String merchantName;

    @Size(max = 50, message = "Merchant city must be at most 50 characters")
    private String merchantCity;

    @Size(max = 10, message = "Merchant ZIP must be at most 10 characters")
    private String merchantZip;

    private String authStatus;
    private String authCode;
    private String declineReason;
    private LocalDateTime requestTs;
    private LocalDateTime responseTs;
    private LocalDateTime createdAt;
}
