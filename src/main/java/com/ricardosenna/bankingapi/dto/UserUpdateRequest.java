package com.ricardosenna.bankingapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 120, message = "Name must have at most 120 characters") String name,
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format") String email
) {}
