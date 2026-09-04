package com.ricardosenna.bankingapi.controller;

import com.ricardosenna.bankingapi.dto.*;
import com.ricardosenna.bankingapi.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/users/me")
@Tag(name = "Users", description = "Authenticated user profile and password")
public class UserController {
    private final AuthService authService;
    public UserController(AuthService authService) { this.authService = authService; }

    @PutMapping
    public ResponseEntity<UserResponse> update(Authentication authentication,
                                               @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(authService.updateCurrentUser(authentication.getName(), request));
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(Authentication authentication,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
