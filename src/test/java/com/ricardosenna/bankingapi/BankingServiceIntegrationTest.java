package com.ricardosenna.bankingapi;

import com.ricardosenna.bankingapi.dto.*;
import com.ricardosenna.bankingapi.enums.AccountType;
import com.ricardosenna.bankingapi.enums.AccountStatus;
import com.ricardosenna.bankingapi.enums.TransactionType;
import com.ricardosenna.bankingapi.exception.BusinessRuleException;
import com.ricardosenna.bankingapi.exception.DuplicateResourceException;
import com.ricardosenna.bankingapi.exception.ResourceNotFoundException;
import com.ricardosenna.bankingapi.service.BankingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDate;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BankingServiceIntegrationTest {

    @Autowired
    private BankingService bankingService;

    @BeforeEach
    void setUp() {
        // Create a test client
        bankingService.createClient(new ClientCreateRequest("12345678901", "Test Client", "test@example.com"));
    }

    @Test
    void shouldCreateClient() {
        var client = bankingService.findByCpf("12345678901");
        assertEquals("Test Client", client.name());
        assertEquals("ACTIVE", client.status());
    }

    @Test
    void shouldRejectDuplicateCpf() {
        assertThrows(DuplicateResourceException.class, () ->
                bankingService.createClient(new ClientCreateRequest("12345678901", "Another Client", "another@example.com"))
        );
    }

    @Test
    void shouldThrowNotFoundForUnknownCpf() {
        assertThrows(ResourceNotFoundException.class, () ->
                bankingService.findByCpf("99999999999")
        );
    }

    @Test
    void shouldCreateCheckingAccount() {
        var account = bankingService.createAccount(
                new AccountCreateRequest("12345678901", AccountType.CHECKING, BigDecimal.valueOf(2.00), null)
        );

        assertNotNull(account.accountNumber());
        assertEquals("CHECKING", account.type());
        assertEquals(BigDecimal.ZERO, account.balance());
    }

    @Test
    void shouldGenerateDifferentAccountNumbersForNewAccounts() {
        var checking = bankingService.createAccount(
                new AccountCreateRequest("12345678901", AccountType.CHECKING, BigDecimal.ZERO, null));
        var savings = bankingService.createAccount(
                new AccountCreateRequest("12345678901", AccountType.SAVINGS, BigDecimal.ZERO, null));

        assertNotEquals(checking.accountNumber(), savings.accountNumber());
        assertTrue(checking.accountNumber() >= 100_000);
        assertTrue(savings.accountNumber() >= 100_000);
    }

    @Test
    void shouldCreateSavingsAccount() {
        var account = bankingService.createAccount(
                new AccountCreateRequest("12345678901", AccountType.SAVINGS, null, BigDecimal.valueOf(0.05))
        );

        assertNotNull(account.accountNumber());
        assertEquals("SAVINGS", account.type());
        assertEquals(BigDecimal.valueOf(0.05).compareTo(account.interestRate()), 0);
    }

    @Test
    void shouldDepositAndCheckBalance() {
        var account = bankingService.createAccount(
                new AccountCreateRequest("12345678901", AccountType.CHECKING, BigDecimal.ZERO, null)
        );

        bankingService.deposit(new DepositRequest(account.accountNumber(), BigDecimal.valueOf(100.00)));

        var updated = bankingService.findByAccountNumber(account.accountNumber());
        assertEquals(0, BigDecimal.valueOf(100.00).compareTo(updated.balance()));
    }

    @Test
    void shouldWithdrawSuccessfully() {
        var account = bankingService.createAccount(
                new AccountCreateRequest("12345678901", AccountType.CHECKING, BigDecimal.ZERO, null)
        );

        bankingService.deposit(new DepositRequest(account.accountNumber(), BigDecimal.valueOf(100.00)));
        bankingService.withdraw(new WithdrawRequest(account.accountNumber(), BigDecimal.valueOf(30.00)));

        var updated = bankingService.findByAccountNumber(account.accountNumber());
        assertEquals(0, BigDecimal.valueOf(70.00).compareTo(updated.balance()));
    }

    @Test
    void shouldRejectWithdrawWithInsufficientBalance() {
        var account = bankingService.createAccount(
                new AccountCreateRequest("12345678901", AccountType.CHECKING, BigDecimal.ZERO, null)
        );

        bankingService.deposit(new DepositRequest(account.accountNumber(), BigDecimal.valueOf(50.00)));

        assertThrows(BusinessRuleException.class, () ->
                bankingService.withdraw(new WithdrawRequest(account.accountNumber(), BigDecimal.valueOf(100.00)))
        );
    }

    @Test
    void shouldTransferBetweenAccounts() {
        var source = bankingService.createAccount(
                new AccountCreateRequest("12345678901", AccountType.CHECKING, BigDecimal.ZERO, null)
        );

        bankingService.createClient(new ClientCreateRequest("98765432100", "Second Client", "second@example.com"));
        var target = bankingService.createAccount(
                new AccountCreateRequest("98765432100", AccountType.CHECKING, BigDecimal.ZERO, null)
        );

        bankingService.deposit(new DepositRequest(source.accountNumber(), BigDecimal.valueOf(200.00)));
        bankingService.transfer(new TransferRequest(source.accountNumber(), target.accountNumber(), BigDecimal.valueOf(50.00)));

        var sourceUpdated = bankingService.findByAccountNumber(source.accountNumber());
        var targetUpdated = bankingService.findByAccountNumber(target.accountNumber());

        assertEquals(0, BigDecimal.valueOf(150.00).compareTo(sourceUpdated.balance()));
        assertEquals(0, BigDecimal.valueOf(50.00).compareTo(targetUpdated.balance()));
    }

    @Test
    void shouldRejectTransferToSameAccount() {
        var account = bankingService.createAccount(
                new AccountCreateRequest("12345678901", AccountType.CHECKING, BigDecimal.ZERO, null)
        );

        bankingService.deposit(new DepositRequest(account.accountNumber(), BigDecimal.valueOf(100.00)));

        assertThrows(BusinessRuleException.class, () ->
                bankingService.transfer(new TransferRequest(account.accountNumber(), account.accountNumber(), BigDecimal.valueOf(10.00)))
        );
    }

    @Test
    void shouldBlockClientAndRejectOperations() {
        // Create account first (client must be active)
        var account = bankingService.createAccount(
                new AccountCreateRequest("12345678901", AccountType.CHECKING, BigDecimal.ZERO, null)
        );

        // Deposit some money
        bankingService.deposit(new DepositRequest(account.accountNumber(), BigDecimal.valueOf(100.00)));

        // Now block the client
        bankingService.updateClientStatus("12345678901", new ClientUpdateStatusRequest(ClientUpdateStatusRequest.ClientStatus.BLOCKED));

        // Withdraw should fail because client is blocked
        assertThrows(BusinessRuleException.class, () ->
                bankingService.withdraw(new WithdrawRequest(account.accountNumber(), BigDecimal.valueOf(50.00)))
        );
    }

    @Test
    void shouldListTransactionsInStatement() {
        var account = bankingService.createAccount(
                new AccountCreateRequest("12345678901", AccountType.CHECKING, BigDecimal.ZERO, null)
        );

        bankingService.deposit(new DepositRequest(account.accountNumber(), BigDecimal.valueOf(100.00)));
        bankingService.deposit(new DepositRequest(account.accountNumber(), BigDecimal.valueOf(50.00)));

        var statement = bankingService.getStatement(account.accountNumber(), PageRequest.of(0, 10));

        assertEquals(2, statement.getTotalElements());
    }

    @Test
    void shouldUpdateClientCompletely() {
        var updated = bankingService.updateClient("12345678901", new ClientUpdateRequest("Updated Client", "updated@example.com"));
        assertEquals("Updated Client", updated.name());
        assertEquals("updated@example.com", updated.email());
    }

    @Test
    void shouldRejectUpdateForUnknownClient() {
        assertThrows(ResourceNotFoundException.class, () -> bankingService.updateClient("99999999999", new ClientUpdateRequest("Unknown", "unknown@example.com")));
    }

    @Test
    void shouldBlockAndReactivateAccount() {
        var account = bankingService.createAccount(new AccountCreateRequest("12345678901", AccountType.CHECKING, BigDecimal.ZERO, null));
        bankingService.updateAccountStatus(account.accountNumber(), new AccountUpdateStatusRequest(AccountStatus.BLOCKED));
        assertThrows(BusinessRuleException.class, () -> bankingService.deposit(new DepositRequest(account.accountNumber(), BigDecimal.TEN)));
        var active = bankingService.updateAccountStatus(account.accountNumber(), new AccountUpdateStatusRequest(AccountStatus.ACTIVE));
        assertEquals("ACTIVE", active.status());
    }

    @Test
    void shouldCloseOnlyZeroBalanceAccount() {
        var account = bankingService.createAccount(new AccountCreateRequest("12345678901", AccountType.CHECKING, BigDecimal.ZERO, null));
        bankingService.deposit(new DepositRequest(account.accountNumber(), BigDecimal.TEN));
        assertThrows(BusinessRuleException.class, () -> bankingService.closeAccount(account.accountNumber()));
        bankingService.withdraw(new WithdrawRequest(account.accountNumber(), BigDecimal.TEN));
        bankingService.closeAccount(account.accountNumber());
        assertEquals("CLOSED", bankingService.findByAccountNumber(account.accountNumber()).status());
    }

    @Test
    void shouldFilterStatementByType() {
        var account = bankingService.createAccount(new AccountCreateRequest("12345678901", AccountType.CHECKING, BigDecimal.ZERO, null));
        bankingService.deposit(new DepositRequest(account.accountNumber(), BigDecimal.valueOf(100)));
        bankingService.withdraw(new WithdrawRequest(account.accountNumber(), BigDecimal.valueOf(20)));
        var statement = bankingService.getStatement(account.accountNumber(), TransactionType.DEPOSIT, null, null, PageRequest.of(0, 10));
        assertEquals(1, statement.getTotalElements());
        assertEquals("DEPOSIT", statement.getContent().get(0).type());
    }
}
