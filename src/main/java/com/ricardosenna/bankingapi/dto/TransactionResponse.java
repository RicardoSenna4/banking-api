package com.ricardosenna.bankingapi.dto;

import com.ricardosenna.bankingapi.entity.TransactionEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        LocalDateTime moment,
        String type,
        String status,
        BigDecimal amount,
        Integer sourceAccountNumber,
        Integer targetAccountNumber,
        Integer accountNumber
) {
    public static TransactionResponse from(TransactionEntity transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getMoment(),
                transaction.getType().name(),
                transaction.getStatus().name(),
                transaction.getAmount(),
                transaction.getSourceAccountNumber(),
                transaction.getTargetAccountNumber(),
                transaction.getAccount().getAccountNumber()
        );
    }
}
