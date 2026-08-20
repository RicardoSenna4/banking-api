package com.ricardosenna.bankingapi.dto;

import com.ricardosenna.bankingapi.entity.ClientEntity;

import java.time.Instant;

public record ClientResponse(
        Long id,
        String cpf,
        String name,
        String email,
        String status,
        Instant createdAt
) {
    public static ClientResponse from(ClientEntity client) {
        return new ClientResponse(
                client.getId(),
                client.getCpf(),
                client.getName(),
                client.getEmail(),
                client.getStatus().name(),
                client.getCreatedAt()
        );
    }
}
