package com.ricardosenna.bankingapi.dto;

import jakarta.validation.constraints.NotNull;

public record ClientUpdateStatusRequest(
        @NotNull(message = "Status is required")
        ClientStatus status
) {
    public enum ClientStatus {
        ACTIVE, BLOCKED
    }
}
