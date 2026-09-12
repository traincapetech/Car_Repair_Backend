package com.carservice.backend.marketplace.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workshop_wallets")
public class WorkshopWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workshop_id", nullable = false, unique = true)
    private Workshop workshop;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private com.carservice.backend.marketplace.enums.WalletStatus status = com.carservice.backend.marketplace.enums.WalletStatus.ACTIVE;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<WalletTransaction> transactions = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public WorkshopWallet() {
    }

    public WorkshopWallet(Workshop workshop, BigDecimal initialBalance) {
        this.workshop = workshop;
        this.balance = initialBalance != null ? initialBalance.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.currency = "INR";
        this.status = com.carservice.backend.marketplace.enums.WalletStatus.ACTIVE;
    }

    public WorkshopWallet(Workshop workshop, BigDecimal initialBalance, String currency, com.carservice.backend.marketplace.enums.WalletStatus status) {
        this.workshop = workshop;
        this.balance = initialBalance != null ? initialBalance.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency != null ? currency : "INR";
        this.status = status != null ? status : com.carservice.backend.marketplace.enums.WalletStatus.ACTIVE;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.balance == null) {
            this.balance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (this.currency == null) {
            this.currency = "INR";
        }
        if (this.status == null) {
            this.status = com.carservice.backend.marketplace.enums.WalletStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void credit(BigDecimal amount) {
        if (this.status != com.carservice.backend.marketplace.enums.WalletStatus.ACTIVE) {
            throw new IllegalStateException("Cannot credit wallet in status: " + this.status);
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        this.balance = this.balance.add(amount).setScale(2, RoundingMode.HALF_UP);
    }

    public void debit(BigDecimal amount) {
        if (this.status != com.carservice.backend.marketplace.enums.WalletStatus.ACTIVE) {
            throw new IllegalStateException("Cannot debit wallet in status: " + this.status);
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient wallet balance. Available: ₹" + this.balance + ", Required: ₹" + amount);
        }
        this.balance = this.balance.subtract(amount).setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Workshop getWorkshop() {
        return workshop;
    }

    public void setWorkshop(Workshop workshop) {
        this.workshop = workshop;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance != null ? balance.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public List<WalletTransaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<WalletTransaction> transactions) {
        this.transactions = transactions;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public com.carservice.backend.marketplace.enums.WalletStatus getStatus() {
        return status;
    }

    public void setStatus(com.carservice.backend.marketplace.enums.WalletStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
