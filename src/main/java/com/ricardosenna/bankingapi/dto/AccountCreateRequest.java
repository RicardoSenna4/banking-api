package com.ricardosenna.bankingapi.dto;

import com.ricardosenna.bankingapi.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AccountCreateRequest(
        @NotBlank(message = "CPF is required")
        String cpf,

        @NotNull(message = "Account type is required")
        AccountType type,

        BigDecimal withdrawFee,
        BigDecimal interestRate
) {}
