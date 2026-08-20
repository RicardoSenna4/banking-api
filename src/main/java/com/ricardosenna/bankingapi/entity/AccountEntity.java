package com.ricardosenna.bankingapi.entity;

import com.ricardosenna.bankingapi.enums.AccountType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

@Entity
@Table(name = "accounts")
public class AccountEntity {

    private static final AtomicInteger ACCOUNT_NUMBER_COUNTER = new AtomicInteger(1000);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, updatable = false)
    private Integer accountNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "withdraw_fee", nullable = false, precision = 19, scale = 4)
    private BigDecimal withdrawFee;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AccountEntity() {}

    public AccountEntity(ClientEntity client, AccountType type,
                         BigDecimal withdrawFee, BigDecimal interestRate) {
        this.client = client;
        this.type = type;
        this.balance = BigDecimal.ZERO;
        this.withdrawFee = withdrawFee;
        this.interestRate = interestRate;
        this.accountNumber = ACCOUNT_NUMBER_COUNTER.incrementAndGet();
        this.createdAt = Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.accountNumber == null) {
            this.accountNumber = ACCOUNT_NUMBER_COUNTER.incrementAndGet();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getAccountNumber() { return accountNumber; }
    public void setAccountNumber(Integer accountNumber) { this.accountNumber = accountNumber; }

    public ClientEntity getClient() { return client; }
    public void setClient(ClientEntity client) { this.client = client; }

    public AccountType getType() { return type; }
    public void setType(AccountType type) { this.type = type; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public BigDecimal getWithdrawFee() { return withdrawFee; }
    public void setWithdrawFee(BigDecimal withdrawFee) { this.withdrawFee = withdrawFee; }

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }

    public Instant getCreatedAt() { return createdAt; }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }

    public boolean hasSufficientBalance(BigDecimal amount) {
        return this.balance.compareTo(amount) >= 0;
    }
}
