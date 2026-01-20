package com.carddemo.authorization.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationRequestDto {
    private String cardNumber;
    private String merchantId;
    private String merchantName;
    private String merchantCategoryCode;
    private String merchantCity;
    private String merchantState;
    private String merchantCountry;
    private BigDecimal amount;
    private String currencyCode;
    private String transactionType;
    private String posEntryMode;
}
