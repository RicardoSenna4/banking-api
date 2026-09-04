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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.ricardosenna.bankingapi.entity.UserEntity;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clients", description = "Client profiles and status management")
public class ClientController {

    private final BankingService bankingService;

    public ClientController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    @PostMapping
    public ResponseEntity<ClientResponse> create(@AuthenticationPrincipal UserEntity user, @Valid @RequestBody ClientCreateRequest request) {
        ClientResponse response = bankingService.createClient(user, request);

        var location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{cpf}")
                .buildAndExpand(response.cpf())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{cpf}")
    public ResponseEntity<ClientResponse> findByCpf(@AuthenticationPrincipal UserEntity user, @PathVariable String cpf) {
        return ResponseEntity.ok(bankingService.findByCpf(user, cpf));
    }

    @PutMapping("/{cpf}")
    public ResponseEntity<ClientResponse> update(@AuthenticationPrincipal UserEntity user, @PathVariable String cpf,
                                                 @Valid @RequestBody ClientUpdateRequest request) {
        return ResponseEntity.ok(bankingService.updateClient(user, cpf, request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ClientResponse>> list(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(bankingService.listClients(pageable));
    }

    @PatchMapping("/{cpf}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClientResponse> updateStatus(
            @PathVariable String cpf,
            @Valid @RequestBody ClientUpdateStatusRequest request) {
        return ResponseEntity.ok(bankingService.updateClientStatus(cpf, request));
    }
}
