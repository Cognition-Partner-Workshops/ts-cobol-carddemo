package com.carddemo.card.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardDto {
    private String cardNumber;
    private String maskedCardNumber;
    private String accountId;
    private String embossedName;
    private LocalDate expirationDate;
    private String activeStatus;
    private String customerId;
    private boolean expired;
}
