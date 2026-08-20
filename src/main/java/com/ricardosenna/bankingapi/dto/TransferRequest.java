package com.ricardosenna.bankingapi.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferRequest(
        @NotNull(message = "Source account number is required")
        Integer sourceAccountNumber,

        @NotNull(message = "Target account number is required")
        Integer targetAccountNumber,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal amount
) {}
