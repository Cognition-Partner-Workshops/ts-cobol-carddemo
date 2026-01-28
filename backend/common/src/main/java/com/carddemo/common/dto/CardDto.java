package com.carddemo.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CardDto {
    private String cardNumber;
    private String maskedCardNumber;
    private Long accountId;
    private String embossedName;
    private LocalDate expirationDate;
    private String activeStatus;
    private Boolean isExpired;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
