package com.carddemo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Transaction type response DTO.
 * Maps from TransactionType entity (CVTRA03Y copybook).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionTypeResponse {

    private String typeCode;
    private String description;
}
