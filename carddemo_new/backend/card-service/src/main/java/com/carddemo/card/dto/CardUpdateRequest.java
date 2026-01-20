package com.carddemo.card.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardUpdateRequest {
    @Size(max = 50, message = "Embossed name must be at most 50 characters")
    @Pattern(regexp = "^[A-Za-z\\s]*$", message = "Embossed name must contain only alphabetic characters")
    private String embossedName;

    private LocalDate expirationDate;

    @Pattern(regexp = "^[YN]$", message = "Active status must be Y or N")
    private String activeStatus;
}
