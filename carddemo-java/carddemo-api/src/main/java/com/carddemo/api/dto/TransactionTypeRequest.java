package com.carddemo.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Transaction type create/update request DTO.
 * Used for admin transaction type management (COTRTUPC).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionTypeRequest {

    @NotBlank(message = "Type code is required")
    @Size(max = 2, message = "Type code must not exceed 2 characters")
    private String typeCode;

    @NotBlank(message = "Description is required")
    @Size(max = 50, message = "Description must not exceed 50 characters")
    private String description;
}
