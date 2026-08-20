package com.ricardosenna.bankingapi.dto;

public record TokenResponse(
        String accessToken,
        long expiresIn
) {}
