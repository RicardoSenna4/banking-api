package com.ricardosenna.bankingapi.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
    public TokenResponse(String accessToken, long expiresIn) {
        this(accessToken, null, expiresIn);
    }
}
