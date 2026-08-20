package com.ricardosenna.bankingapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClientCreateRequest(
        @NotBlank(message = "CPF is required")
        @Pattern(regexp = "\\d{11}", message = "CPF must contain exactly 11 digits")
        String cpf,

        @NotBlank(message = "Name is required")
        @Size(max = 120) String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email
) {}
