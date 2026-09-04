package com.ricardosenna.bankingapi.controller;

import com.ricardosenna.bankingapi.dto.AccountCreateRequest;
import com.ricardosenna.bankingapi.dto.AccountResponse;
import com.ricardosenna.bankingapi.dto.AccountUpdateStatusRequest;
import com.ricardosenna.bankingapi.service.BankingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.ricardosenna.bankingapi.entity.UserEntity;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "Bank account lifecycle and ownership")
public class AccountController {

    private final BankingService bankingService;

    public AccountController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@AuthenticationPrincipal UserEntity user, @Valid @RequestBody AccountCreateRequest request) {
        AccountResponse response = bankingService.createAccount(user, request);

        var location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{number}")
                .buildAndExpand(response.accountNumber())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{number}")
    public ResponseEntity<AccountResponse> findByNumber(@AuthenticationPrincipal UserEntity user, @PathVariable Integer number) {
        return ResponseEntity.ok(bankingService.findByAccountNumber(user, number));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> listByClient(@AuthenticationPrincipal UserEntity user, @RequestParam Long clientId) {
        return ResponseEntity.ok(bankingService.listAccountsByClient(user, clientId));
    }

    @PatchMapping("/{number}/status")
    public ResponseEntity<AccountResponse> updateStatus(@AuthenticationPrincipal UserEntity user, @PathVariable Integer number,
                                                        @Valid @RequestBody AccountUpdateStatusRequest request) {
        return ResponseEntity.ok(bankingService.updateAccountStatus(user, number, request));
    }

    @DeleteMapping("/{number}")
    public ResponseEntity<Void> close(@AuthenticationPrincipal UserEntity user, @PathVariable Integer number) {
        bankingService.closeAccount(user, number);
        return ResponseEntity.noContent().build();
    }
}
