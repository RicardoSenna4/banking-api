package com.ricardosenna.bankingapi.entity;

import com.ricardosenna.bankingapi.enums.TransactionStatus;
import com.ricardosenna.bankingapi.enums.TransactionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime moment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private TransactionStatus status;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "source_account_number")
    private Integer sourceAccountNumber;

    @Column(name = "target_account_number")
    private Integer targetAccountNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public TransactionEntity() {}

    public TransactionEntity(LocalDateTime moment, TransactionType type,
                             TransactionStatus status, BigDecimal amount,
                             Integer sourceAccountNumber, Integer targetAccountNumber,
                             AccountEntity account) {
        this.moment = moment;
        this.type = type;
        this.status = status;
        this.amount = amount;
        this.sourceAccountNumber = sourceAccountNumber;
        this.targetAccountNumber = targetAccountNumber;
        this.account = account;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getMoment() { return moment; }
    public void setMoment(LocalDateTime moment) { this.moment = moment; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Integer getSourceAccountNumber() { return sourceAccountNumber; }
    public void setSourceAccountNumber(Integer sourceAccountNumber) { this.sourceAccountNumber = sourceAccountNumber; }

    public Integer getTargetAccountNumber() { return targetAccountNumber; }
    public void setTargetAccountNumber(Integer targetAccountNumber) { this.targetAccountNumber = targetAccountNumber; }

    public AccountEntity getAccount() { return account; }
    public void setAccount(AccountEntity account) { this.account = account; }

    public Instant getCreatedAt() { return createdAt; }
}
