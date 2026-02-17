package com.aws.carddemo.card.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CardUpdateRequest(
        @Pattern(regexp = "[ACL]", message = "Card status must be A (Active), C (Cancelled), or L (Lost)")
        String cardStatus,

        @Size(max = 50, message = "Embossed name must not exceed 50 characters")
        String embossedName,

        @Future(message = "Expiry date must be in the future")
        LocalDate expiryDate
) {}
