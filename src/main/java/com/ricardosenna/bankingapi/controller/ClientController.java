package com.ricardosenna.bankingapi.controller;

import com.ricardosenna.bankingapi.dto.*;
import com.ricardosenna.bankingapi.service.BankingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final BankingService bankingService;

    public ClientController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    @PostMapping
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientCreateRequest request) {
        ClientResponse response = bankingService.createClient(request);

        var location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{cpf}")
                .buildAndExpand(response.cpf())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{cpf}")
    public ResponseEntity<ClientResponse> findByCpf(@PathVariable String cpf) {
        return ResponseEntity.ok(bankingService.findByCpf(cpf));
    }

    @GetMapping
    public ResponseEntity<Page<ClientResponse>> list(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(bankingService.listClients(pageable));
    }

    @PatchMapping("/{cpf}/status")
    public ResponseEntity<ClientResponse> updateStatus(
            @PathVariable String cpf,
            @Valid @RequestBody ClientUpdateStatusRequest request) {
        return ResponseEntity.ok(bankingService.updateClientStatus(cpf, request));
    }
}
