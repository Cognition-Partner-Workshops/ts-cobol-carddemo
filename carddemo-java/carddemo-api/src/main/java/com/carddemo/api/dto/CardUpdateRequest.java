package com.carddemo.api.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Card update request DTO replacing BMS map COCRDUP input fields.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardUpdateRequest {

    @Size(max = 50, message = "Embossed name must not exceed 50 characters")
    private String embossedName;

    private LocalDate expirationDate;

    @Size(max = 1, message = "Active status must be 1 character")
    private String activeStatus;
}
