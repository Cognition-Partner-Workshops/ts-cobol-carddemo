package com.carddemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {
    
    private String accountId;
    
    @NotBlank(message = "Card number is required")
    private String cardNumber;
    
    @NotBlank(message = "Type code is required")
    private String typeCode;
    
    @NotNull(message = "Category code is required")
    private Integer categoryCode;
    
    @NotBlank(message = "Source is required")
    private String source;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    @NotNull(message = "Amount is required")
    private BigDecimal amount;
    
    @NotBlank(message = "Merchant ID is required")
    private String merchantId;
    
    @NotBlank(message = "Merchant name is required")
    private String merchantName;
    
    @NotBlank(message = "Merchant city is required")
    private String merchantCity;
    
    @NotBlank(message = "Merchant zip is required")
    private String merchantZip;
}
