package com.aws.carddemo.statement.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record StatementRequest(
        @NotNull Long accountId,
        @NotNull LocalDate periodStartDate,
        @NotNull LocalDate periodEndDate
) {}
