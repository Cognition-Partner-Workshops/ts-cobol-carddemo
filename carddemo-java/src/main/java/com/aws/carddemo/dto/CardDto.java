package com.aws.carddemo.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardDto {

    @NotBlank(message = "Card number is required")
    @Size(max = 16, message = "Card number must be at most 16 characters")
    private String cardNum;

    @NotNull(message = "Account ID is required")
    private Long cardAcctId;

    @NotBlank(message = "CVV is required")
    @Size(max = 3, message = "CVV must be at most 3 characters")
    private String cardCvvCd;

    @Size(max = 50, message = "Embossed name must be at most 50 characters")
    private String cardEmbossedName;

    @NotNull(message = "Expiration date is required")
    private LocalDate cardExpirationDate;

    @Size(max = 1, message = "Active status must be 1 character")
    private String cardActiveStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String maskedCardNumber;
    private boolean active;
    private boolean expired;
}
