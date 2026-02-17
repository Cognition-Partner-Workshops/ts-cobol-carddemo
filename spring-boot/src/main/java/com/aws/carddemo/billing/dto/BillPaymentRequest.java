package com.aws.carddemo.billing.dto;

import jakarta.validation.constraints.NotNull;

public record BillPaymentRequest(
        @NotNull(message = "Account ID is required")
        Long accountId,

        @NotNull(message = "Confirmation is required")
        Boolean confirmed
) {}
