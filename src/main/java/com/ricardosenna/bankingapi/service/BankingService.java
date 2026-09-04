package com.ricardosenna.bankingapi.service;

import com.ricardosenna.bankingapi.dto.*;
import com.ricardosenna.bankingapi.entity.*;
import com.ricardosenna.bankingapi.enums.*;
import com.ricardosenna.bankingapi.exception.*;
import com.ricardosenna.bankingapi.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

@Service
public class BankingService {
    private final ClientRepository clientRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public BankingService(ClientRepository clientRepository, AccountRepository accountRepository,
                          TransactionRepository transactionRepository) {
        this.clientRepository = clientRepository; this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public ClientResponse createClient(ClientCreateRequest request) {
        return createClient(null, request);
    }
    @Transactional
    public ClientResponse createClient(UserEntity user, ClientCreateRequest request) {
        String cpf = sanitizeCpf(request.cpf());
        if (clientRepository.existsByCpf(cpf)) throw new DuplicateResourceException("A client with CPF " + cpf + " already exists");
        if (user != null && clientRepository.existsByUserId(user.getId())) throw new BusinessRuleException("User already has a client profile");
        ClientEntity client = new ClientEntity(user, cpf, request.name().trim(), request.email().toLowerCase().trim(), ClientEntity.ClientStatus.ACTIVE);
        return ClientResponse.from(clientRepository.save(client));
    }
    @Transactional @Cacheable(value = "clients", key = "#cpf") public ClientResponse findByCpf(String cpf) {
        return ClientResponse.from(clientRepository.findByCpf(sanitizeCpf(cpf))
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with CPF: " + cpf)));
    }
    @Transactional public ClientResponse findByCpf(UserEntity user, String cpf) {
        ClientEntity client = clientRepository.findByCpf(sanitizeCpf(cpf)).orElseThrow(() -> new ResourceNotFoundException("Client not found with CPF: " + cpf));
        validateClientOwnership(client, user);
        return ClientResponse.from(client);
    }
    @Transactional public Page<ClientResponse> listClients(Pageable pageable) { return clientRepository.findAll(pageable).map(ClientResponse::from); }
    @Transactional @CacheEvict(value = "clients", key = "#cpf") public ClientResponse updateClient(String cpf, ClientUpdateRequest request) {
        return updateClient(null, cpf, request);
    }
    @Transactional @CacheEvict(value = "clients", key = "#cpf") public ClientResponse updateClient(UserEntity user, String cpf, ClientUpdateRequest request) {
        ClientEntity client = clientRepository.findByCpf(sanitizeCpf(cpf)).orElseThrow(() -> new ResourceNotFoundException("Client not found with CPF: " + cpf));
        validateClientOwnership(client, user);
        client.setName(request.name().trim()); client.setEmail(request.email().toLowerCase().trim());
        return ClientResponse.from(clientRepository.save(client));
    }
    @Transactional @CacheEvict(value = "clients", key = "#cpf") public ClientResponse updateClientStatus(String cpf, ClientUpdateStatusRequest request) {
        ClientEntity client = clientRepository.findByCpf(sanitizeCpf(cpf)).orElseThrow(() -> new ResourceNotFoundException("Client not found with CPF: " + cpf));
        if (request.status() == ClientUpdateStatusRequest.ClientStatus.ACTIVE) client.activate(); else client.block();
        return ClientResponse.from(clientRepository.save(client));
    }

    @Transactional public AccountResponse createAccount(AccountCreateRequest request) {
        return createAccount(null, request);
    }
    @Transactional public AccountResponse createAccount(UserEntity user, AccountCreateRequest request) {
        ClientEntity client = clientRepository.findByCpf(sanitizeCpf(request.cpf())).orElseThrow(() -> new ResourceNotFoundException("Client not found with CPF: " + request.cpf()));
        validateClientOwnership(client, user);
        validateActiveClient(client);
        AccountEntity account = new AccountEntity(client, request.type(), request.withdrawFee() != null ? request.withdrawFee() : BigDecimal.ZERO, request.interestRate() != null ? request.interestRate() : BigDecimal.ZERO);
        return AccountResponse.from(accountRepository.save(account));
    }
    @Transactional public AccountResponse findByAccountNumber(Integer n) { return AccountResponse.from(findAccountOrThrow(n)); }
    @Transactional public AccountResponse findByAccountNumber(UserEntity user, Integer n) { AccountEntity account = findAccountOrThrow(n); validateAccountOwnership(account, user); return AccountResponse.from(account); }
    @Transactional public List<AccountResponse> listAccountsByClient(Long clientId) { return accountRepository.findAllByClientId(clientId).stream().map(AccountResponse::from).toList(); }
    @Transactional public List<AccountResponse> listAccountsByClient(UserEntity user, Long clientId) {
        ClientEntity client = clientRepository.findById(clientId).orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));
        validateClientOwnership(client, user);
        return listAccountsByClient(clientId);
    }
    @Transactional public AccountResponse updateAccountStatus(Integer number, AccountUpdateStatusRequest request) {
        return updateAccountStatus(null, number, request);
    }
    @Transactional public AccountResponse updateAccountStatus(UserEntity user, Integer number, AccountUpdateStatusRequest request) {
        AccountEntity account = findAccountOrThrow(number);
        validateAccountOwnership(account, user);
        if (account.getStatus() == AccountStatus.CLOSED) throw new BusinessRuleException("Closed account cannot change status");
        account.setStatus(request.status());
        return AccountResponse.from(accountRepository.save(account));
    }
    @Transactional public void closeAccount(Integer number) {
        closeAccount(null, number);
    }
    @Transactional public void closeAccount(UserEntity user, Integer number) {
        AccountEntity account = findAccountOrThrow(number);
        validateAccountOwnership(account, user);
        if (account.getStatus() == AccountStatus.CLOSED) throw new BusinessRuleException("Account is already closed");
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) throw new BusinessRuleException("Account must have zero balance before closing");
        account.setStatus(AccountStatus.CLOSED); accountRepository.save(account);
    }

    @Transactional public TransactionResponse deposit(DepositRequest request) {
        return deposit(null, request);
    }
    @Transactional public TransactionResponse deposit(UserEntity user, DepositRequest request) {
        AccountEntity account = findAccountOrThrow(request.accountNumber()); validateOperationalAccount(account);
        validateAccountOwnership(account, user);
        account.credit(request.amount()); accountRepository.save(account);
        return TransactionResponse.from(transactionRepository.save(new TransactionEntity(LocalDateTime.now(), TransactionType.DEPOSIT, TransactionStatus.APPROVED, request.amount(), null, account.getAccountNumber(), account)));
    }
    @Transactional public TransactionResponse withdraw(WithdrawRequest request) {
        return withdraw(null, request);
    }
    @Transactional public TransactionResponse withdraw(UserEntity user, WithdrawRequest request) {
        AccountEntity account = findAccountOrThrow(request.accountNumber()); validateOperationalAccount(account);
        validateAccountOwnership(account, user);
        BigDecimal amount = request.amount();
        if (account.getType() == AccountType.CHECKING && account.getWithdrawFee().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal total = amount.add(account.getWithdrawFee());
            if (!account.hasSufficientBalance(total)) throw new BusinessRuleException("Insufficient balance to cover amount and withdrawal fee");
            account.debit(total);
        } else { if (!account.hasSufficientBalance(amount)) throw new BusinessRuleException("Insufficient balance"); account.debit(amount); }
        accountRepository.save(account);
        return TransactionResponse.from(transactionRepository.save(new TransactionEntity(LocalDateTime.now(), TransactionType.WITHDRAW, TransactionStatus.APPROVED, amount, account.getAccountNumber(), null, account)));
    }
    @Transactional public TransactionResponse transfer(TransferRequest request) {
        return transfer(null, request);
    }
    @Transactional public TransactionResponse transfer(UserEntity user, TransferRequest request) {
        if (request.sourceAccountNumber().equals(request.targetAccountNumber())) throw new BusinessRuleException("Source and target accounts must be different");
        AccountEntity source = findAccountOrThrow(request.sourceAccountNumber()), target = findAccountOrThrow(request.targetAccountNumber());
        validateAccountOwnership(source, user);
        validateOperationalAccount(source); validateOperationalAccount(target);
        if (!source.hasSufficientBalance(request.amount())) throw new BusinessRuleException("Insufficient balance for transfer");
        source.debit(request.amount()); target.credit(request.amount()); accountRepository.save(source); accountRepository.save(target);
        LocalDateTime now = LocalDateTime.now();
        TransactionEntity sourceTx = new TransactionEntity(now, TransactionType.TRANSFER_OUT, TransactionStatus.APPROVED, request.amount(), source.getAccountNumber(), target.getAccountNumber(), source);
        TransactionEntity targetTx = new TransactionEntity(now, TransactionType.TRANSFER_IN, TransactionStatus.APPROVED, request.amount(), source.getAccountNumber(), target.getAccountNumber(), target);
        transactionRepository.save(sourceTx); transactionRepository.save(targetTx); return TransactionResponse.from(sourceTx);
    }
    @Transactional public Page<TransactionResponse> getStatement(Integer number, Pageable pageable) { return getStatement(number, null, null, null, pageable); }
    @Transactional public Page<TransactionResponse> getStatement(UserEntity user, Integer number, TransactionType type, LocalDate start, LocalDate end, Pageable pageable) { AccountEntity account = findAccountOrThrow(number); validateAccountOwnership(account, user); return getStatement(number, type, start, end, pageable); }
    @Transactional public Page<TransactionResponse> getStatement(Integer number, TransactionType type, LocalDate start, LocalDate end, Pageable pageable) {
        if (start != null && end != null && end.isBefore(start)) throw new BusinessRuleException("endDate must be on or after startDate");
        AccountEntity account = findAccountOrThrow(number);
        LocalDateTime startDate = start == null ? null : start.atStartOfDay();
        LocalDateTime endDate = end == null ? null : end.plusDays(1).atStartOfDay();
        return transactionRepository.findStatement(account.getId(), type, startDate, endDate, pageable).map(TransactionResponse::from);
    }

    private AccountEntity findAccountOrThrow(Integer n) { return accountRepository.findByAccountNumber(n).orElseThrow(() -> new ResourceNotFoundException("Account not found with number: " + n)); }
    private void validateActiveClient(ClientEntity c) { if (c.getStatus() != ClientEntity.ClientStatus.ACTIVE) throw new BusinessRuleException("Client is blocked and cannot perform operations"); }
    private void validateOperationalAccount(AccountEntity a) { validateActiveClient(a.getClient()); if (!a.isActive()) throw new BusinessRuleException("Account is not active and cannot perform operations"); }
    private void validateClientOwnership(ClientEntity client, UserEntity user) { if (user != null && user.getRole() != UserEntity.Role.ADMIN && (client.getUser() == null || !user.getId().equals(client.getUser().getId()))) throw new org.springframework.security.access.AccessDeniedException("You are not allowed to access this client"); }
    private void validateAccountOwnership(AccountEntity account, UserEntity user) { if (user != null && user.getRole() != UserEntity.Role.ADMIN) validateClientOwnership(account.getClient(), user); }
    private String sanitizeCpf(String cpf) { return cpf == null ? null : cpf.replaceAll("[^0-9]", ""); }
}
