package com.ricardosenna.bankingapi.controller;

import com.ricardosenna.bankingapi.dto.AccountCreateRequest;
import com.ricardosenna.bankingapi.dto.AccountResponse;
import com.ricardosenna.bankingapi.service.BankingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final BankingService bankingService;

    public AccountController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody AccountCreateRequest request) {
        AccountResponse response = bankingService.createAccount(request);

        var location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{number}")
                .buildAndExpand(response.accountNumber())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{number}")
    public ResponseEntity<AccountResponse> findByNumber(@PathVariable Integer number) {
        return ResponseEntity.ok(bankingService.findByAccountNumber(number));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> listByClient(@RequestParam Long clientId) {
        return ResponseEntity.ok(bankingService.listAccountsByClient(clientId));
    }
}
