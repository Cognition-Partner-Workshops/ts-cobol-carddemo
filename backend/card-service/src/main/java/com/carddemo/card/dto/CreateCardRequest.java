package com.carddemo.card.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCardRequest {

    @NotNull(message = "Account ID is required")
    private Long accountId;

    @NotBlank(message = "Embossed name is required")
    @Size(max = 50, message = "Embossed name must be at most 50 characters")
    private String embossedName;

    @NotNull(message = "Expiration date is required")
    @Future(message = "Expiration date must be in the future")
    private LocalDate expirationDate;
}
