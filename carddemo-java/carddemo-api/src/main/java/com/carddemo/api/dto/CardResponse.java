package com.carddemo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Card response DTO replacing BMS map COCRDSL output fields.
 * Maps from Card entity (CVACT02Y copybook).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardResponse {

    private String cardNumber;
    private Long accountId;
    private Integer cvvCode;
    private String embossedName;
    private LocalDate expirationDate;
    private String activeStatus;
}
