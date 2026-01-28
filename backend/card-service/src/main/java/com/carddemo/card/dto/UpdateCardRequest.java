package com.carddemo.card.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCardRequest {

    @Size(max = 50, message = "Embossed name must be at most 50 characters")
    private String embossedName;

    private LocalDate expirationDate;

    @Pattern(regexp = "^[YN]$", message = "Active status must be 'Y' or 'N'")
    private String activeStatus;
}
