package com.ricardosenna.bankingapi.controller;

import com.ricardosenna.bankingapi.dto.*;
import com.ricardosenna.bankingapi.service.BankingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import com.ricardosenna.bankingapi.enums.TransactionType;
import com.ricardosenna.bankingapi.entity.UserEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Deposits, withdrawals, transfers and statements")
public class TransactionController {

    private final BankingService bankingService;

    public TransactionController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@AuthenticationPrincipal UserEntity user, @Valid @RequestBody DepositRequest request) {
        return ResponseEntity.status(201).body(bankingService.deposit(user, request));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@AuthenticationPrincipal UserEntity user, @Valid @RequestBody WithdrawRequest request) {
        return ResponseEntity.status(201).body(bankingService.withdraw(user, request));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@AuthenticationPrincipal UserEntity user, @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.status(201).body(bankingService.transfer(user, request));
    }

    @GetMapping("/accounts/{accountNumber}/statement")
    public ResponseEntity<Page<TransactionResponse>> getStatement(
            @AuthenticationPrincipal UserEntity user, @PathVariable Integer accountNumber,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(bankingService.getStatement(user, accountNumber, type, startDate, endDate, pageable));
    }
}
