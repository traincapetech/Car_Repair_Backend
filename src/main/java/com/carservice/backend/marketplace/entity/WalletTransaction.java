package com.carservice.backend.marketplace.entity;

import com.carservice.backend.marketplace.enums.WalletReferenceType;
import com.carservice.backend.marketplace.enums.WalletTransactionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "wallet_transactions",
        indexes = {
                @Index(name = "idx_wt_wallet_id", columnList = "wallet_id"),
                @Index(name = "idx_wt_workshop_id", columnList = "workshop_id"),
                @Index(name = "idx_wt_reference", columnList = "reference_type, reference_id"),
                @Index(name = "idx_wt_reference_id", columnList = "reference_id"),
                @Index(name = "idx_wt_type", columnList = "transaction_type"),
                @Index(name = "idx_wt_created_at", columnList = "created_at"),
                @Index(name = "idx_wt_idempotency", columnList = "idempotency_key")
        }
)
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private WorkshopWallet wallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workshop_id", nullable = false)
    private Workshop workshop;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private WalletTransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 40)
    private WalletReferenceType referenceType = WalletReferenceType.WALLET_TOPUP;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_before", nullable = false, precision = 10, scale = 2)
    private BigDecimal balanceBefore = BigDecimal.ZERO;

    @Column(name = "balance_after", nullable = false, precision = 10, scale = 2)
    private BigDecimal balanceAfter = BigDecimal.ZERO;

    @Column(name = "reference_id", nullable = false, length = 64)
    private String referenceId;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public WalletTransaction() {
    }

    public WalletTransaction(
            WorkshopWallet wallet,
            Workshop workshop,
            WalletTransactionType type,
            WalletReferenceType referenceType,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String referenceId,
            String idempotencyKey,
            String description
    ) {
        this.wallet = wallet;
        this.workshop = workshop != null ? workshop : (wallet != null ? wallet.getWorkshop() : null);
        this.type = type;
        this.referenceType = referenceType != null ? referenceType : WalletReferenceType.WALLET_TOPUP;
        this.amount = amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        this.balanceBefore = balanceBefore != null ? balanceBefore.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        this.balanceAfter = balanceAfter != null ? balanceAfter.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        this.referenceId = referenceId;
        this.idempotencyKey = idempotencyKey;
        this.description = description;
    }

    // Backward-compatible constructor
    public WalletTransaction(
            WorkshopWallet wallet,
            WalletTransactionType type,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String referenceId,
            String description
    ) {
        this.wallet = wallet;
        this.workshop = wallet != null ? wallet.getWorkshop() : null;
        this.type = type;
        this.referenceType = type == WalletTransactionType.DEBIT ? WalletReferenceType.OPPORTUNITY_ACCEPTANCE_FEE : WalletReferenceType.WALLET_TOPUP;
        this.amount = amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        this.balanceAfter = balanceAfter != null ? balanceAfter.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        this.balanceBefore = type == WalletTransactionType.DEBIT ? this.balanceAfter.add(this.amount) : this.balanceAfter.subtract(this.amount).max(BigDecimal.ZERO);
        this.referenceId = referenceId;
        this.description = description;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.workshop == null && this.wallet != null) {
            this.workshop = this.wallet.getWorkshop();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WorkshopWallet getWallet() {
        return wallet;
    }

    public void setWallet(WorkshopWallet wallet) {
        this.wallet = wallet;
    }

    public Workshop getWorkshop() {
        return workshop;
    }

    public void setWorkshop(Workshop workshop) {
        this.workshop = workshop;
    }

    public WalletTransactionType getType() {
        return type;
    }

    public void setType(WalletTransactionType type) {
        this.type = type;
    }

    public WalletReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(WalletReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public void setBalanceBefore(BigDecimal balanceBefore) {
        this.balanceBefore = balanceBefore;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
