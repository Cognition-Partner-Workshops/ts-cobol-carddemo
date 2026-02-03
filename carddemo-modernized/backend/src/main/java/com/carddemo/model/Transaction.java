package com.carddemo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transactions")
public class Transaction {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String transactionId;
    
    private String typeCode;
    private Integer categoryCode;
    private String source;
    private String description;
    private BigDecimal amount;
    
    private String merchantId;
    private String merchantName;
    private String merchantCity;
    private String merchantZip;
    
    @Indexed
    private String cardNumber;
    
    private LocalDateTime originTimestamp;
    private LocalDateTime processTimestamp;
}
