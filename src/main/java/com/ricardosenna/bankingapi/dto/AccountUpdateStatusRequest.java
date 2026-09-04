package com.ricardosenna.bankingapi.dto;

import com.ricardosenna.bankingapi.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record AccountUpdateStatusRequest(
        @NotNull(message = "Status is required") AccountStatus status
) {}
