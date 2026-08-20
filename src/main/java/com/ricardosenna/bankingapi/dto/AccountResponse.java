package com.ricardosenna.bankingapi.dto;

import com.ricardosenna.bankingapi.entity.AccountEntity;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        Long id,
        Integer accountNumber,
        Long clientId,
        String type,
        BigDecimal balance,
        BigDecimal withdrawFee,
        BigDecimal interestRate,
        Instant createdAt
) {
    public static AccountResponse from(AccountEntity account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getClient().getId(),
                account.getType().name(),
                account.getBalance(),
                account.getWithdrawFee(),
                account.getInterestRate(),
                account.getCreatedAt()
        );
    }
}
