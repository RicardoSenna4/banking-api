package com.ricardosenna.bankingapi.controller;

import com.ricardosenna.bankingapi.dto.*;
import com.ricardosenna.bankingapi.service.BankingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final BankingService bankingService;

    public TransactionController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody DepositRequest request) {
        return ResponseEntity.status(201).body(bankingService.deposit(request));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody WithdrawRequest request) {
        return ResponseEntity.status(201).body(bankingService.withdraw(request));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.status(201).body(bankingService.transfer(request));
    }

    @GetMapping("/accounts/{accountNumber}/statement")
    public ResponseEntity<Page<TransactionResponse>> getStatement(
            @PathVariable Integer accountNumber,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(bankingService.getStatement(accountNumber, pageable));
    }
}
