package com.ricardosenna.bankingapi.service;

import com.ricardosenna.bankingapi.dto.*;
import com.ricardosenna.bankingapi.entity.AccountEntity;
import com.ricardosenna.bankingapi.entity.ClientEntity;
import com.ricardosenna.bankingapi.entity.TransactionEntity;
import com.ricardosenna.bankingapi.enums.AccountType;
import com.ricardosenna.bankingapi.enums.TransactionStatus;
import com.ricardosenna.bankingapi.enums.TransactionType;
import com.ricardosenna.bankingapi.exception.BusinessRuleException;
import com.ricardosenna.bankingapi.exception.DuplicateResourceException;
import com.ricardosenna.bankingapi.exception.ResourceNotFoundException;
import com.ricardosenna.bankingapi.repository.AccountRepository;
import com.ricardosenna.bankingapi.repository.ClientRepository;
import com.ricardosenna.bankingapi.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BankingService {

    private final ClientRepository clientRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public BankingService(ClientRepository clientRepository,
                          AccountRepository accountRepository,
                          TransactionRepository transactionRepository) {
        this.clientRepository = clientRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    // ==================== CLIENTS ====================

    @Transactional
    public ClientResponse createClient(ClientCreateRequest request) {
        String cpf = sanitizeCpf(request.cpf());

        if (clientRepository.existsByCpf(cpf)) {
            throw new DuplicateResourceException("A client with CPF " + cpf + " already exists");
        }

        ClientEntity client = new ClientEntity(
                cpf,
                request.name().trim(),
                request.email().toLowerCase().trim(),
                ClientEntity.ClientStatus.ACTIVE
        );

        return ClientResponse.from(clientRepository.save(client));
    }

    @Transactional
    public ClientResponse findByCpf(String cpf) {
        ClientEntity client = clientRepository.findByCpf(sanitizeCpf(cpf))
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with CPF: " + cpf));
        return ClientResponse.from(client);
    }

    @Transactional
    public Page<ClientResponse> listClients(Pageable pageable) {
        return clientRepository.findAll(pageable).map(ClientResponse::from);
    }

    @Transactional
    public ClientResponse updateClientStatus(String cpf, ClientUpdateStatusRequest request) {
        ClientEntity client = clientRepository.findByCpf(sanitizeCpf(cpf))
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with CPF: " + cpf));

        switch (request.status()) {
            case ACTIVE -> client.activate();
            case BLOCKED -> client.block();
        }

        return ClientResponse.from(clientRepository.save(client));
    }

    // ==================== ACCOUNTS ====================

    @Transactional
    public AccountResponse createAccount(AccountCreateRequest request) {
        ClientEntity client = clientRepository.findByCpf(sanitizeCpf(request.cpf()))
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with CPF: " + request.cpf()));

        validateActiveClient(client);

        AccountEntity account = new AccountEntity(
                client,
                request.type(),
                request.withdrawFee() != null ? request.withdrawFee() : BigDecimal.ZERO,
                request.interestRate() != null ? request.interestRate() : BigDecimal.ZERO
        );

        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional
    public AccountResponse findByAccountNumber(Integer accountNumber) {
        AccountEntity account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with number: " + accountNumber));
        return AccountResponse.from(account);
    }

    @Transactional
    public List<AccountResponse> listAccountsByClient(Long clientId) {
        return accountRepository.findAllByClientId(clientId)
                .stream()
                .map(AccountResponse::from)
                .toList();
    }

    // ==================== TRANSACTIONS ====================

    @Transactional
    public TransactionResponse deposit(DepositRequest request) {
        AccountEntity account = findAccountOrThrow(request.accountNumber());
        validateActiveClient(account.getClient());

        BigDecimal amount = request.amount();
        account.credit(amount);
        accountRepository.save(account);

        TransactionEntity transaction = new TransactionEntity(
                LocalDateTime.now(),
                TransactionType.DEPOSIT,
                TransactionStatus.APPROVED,
                amount,
                null,
                account.getAccountNumber(),
                account
        );

        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse withdraw(WithdrawRequest request) {
        AccountEntity account = findAccountOrThrow(request.accountNumber());
        validateActiveClient(account.getClient());

        BigDecimal amount = request.amount();

        if (account.getType() == AccountType.CHECKING && account.getWithdrawFee().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalDebit = amount.add(account.getWithdrawFee());
            if (!account.hasSufficientBalance(totalDebit)) {
                throw new BusinessRuleException("Insufficient balance to cover amount and withdrawal fee");
            }
            account.debit(totalDebit);
        } else {
            if (!account.hasSufficientBalance(amount)) {
                throw new BusinessRuleException("Insufficient balance");
            }
            account.debit(amount);
        }

        accountRepository.save(account);

        TransactionEntity transaction = new TransactionEntity(
                LocalDateTime.now(),
                TransactionType.WITHDRAW,
                TransactionStatus.APPROVED,
                amount,
                account.getAccountNumber(),
                null,
                account
        );

        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        if (request.sourceAccountNumber().equals(request.targetAccountNumber())) {
            throw new BusinessRuleException("Source and target accounts must be different");
        }

        AccountEntity source = findAccountOrThrow(request.sourceAccountNumber());
        AccountEntity target = findAccountOrThrow(request.targetAccountNumber());

        validateActiveClient(source.getClient());
        validateActiveClient(target.getClient());

        BigDecimal amount = request.amount();

        if (!source.hasSufficientBalance(amount)) {
            throw new BusinessRuleException("Insufficient balance for transfer");
        }

        source.debit(amount);
        target.credit(amount);

        accountRepository.save(source);
        accountRepository.save(target);

        // Transaction record on source account
        TransactionEntity sourceTx = new TransactionEntity(
                LocalDateTime.now(),
                TransactionType.TRANSFER_OUT,
                TransactionStatus.APPROVED,
                amount,
                source.getAccountNumber(),
                target.getAccountNumber(),
                source
        );

        // Transaction record on target account
        TransactionEntity targetTx = new TransactionEntity(
                LocalDateTime.now(),
                TransactionType.TRANSFER_IN,
                TransactionStatus.APPROVED,
                amount,
                source.getAccountNumber(),
                target.getAccountNumber(),
                target
        );

        transactionRepository.save(sourceTx);
        transactionRepository.save(targetTx);

        return TransactionResponse.from(sourceTx);
    }

    @Transactional
    public Page<TransactionResponse> getStatement(Integer accountNumber, Pageable pageable) {
        AccountEntity account = findAccountOrThrow(accountNumber);
        return transactionRepository
                .findAllByAccountIdOrderByMomentDesc(account.getId(), pageable)
                .map(TransactionResponse::from);
    }

    // ==================== HELPERS ====================

    private AccountEntity findAccountOrThrow(Integer accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with number: " + accountNumber));
    }

    private void validateActiveClient(ClientEntity client) {
        if (client.getStatus() != ClientEntity.ClientStatus.ACTIVE) {
            throw new BusinessRuleException("Client is blocked and cannot perform operations");
        }
    }

    private String sanitizeCpf(String cpf) {
        if (cpf == null) return null;
        return cpf.replaceAll("[^0-9]", "");
    }
}
