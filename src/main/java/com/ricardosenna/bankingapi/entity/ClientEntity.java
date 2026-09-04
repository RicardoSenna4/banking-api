package com.ricardosenna.bankingapi.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "clients")
public class ClientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private UserEntity user;

    @Column(nullable = false, length = 14, unique = true)
    private String cpf;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 120)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private ClientStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ClientEntity() {}

    public ClientEntity(String cpf, String name, String email, ClientStatus status) {
        this(null, cpf, name, email, status);
    }

    public ClientEntity(UserEntity user, String cpf, String name, String email, ClientStatus status) {
        this.user = user;
        this.cpf = cpf;
        this.name = name;
        this.email = email;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public ClientStatus getStatus() { return status; }
    public void setStatus(ClientStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }

    public void activate() { this.status = ClientStatus.ACTIVE; }
    public void block() { this.status = ClientStatus.BLOCKED; }

    public enum ClientStatus {
        ACTIVE, BLOCKED
    }
}
