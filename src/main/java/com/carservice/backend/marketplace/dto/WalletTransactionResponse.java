package com.carservice.backend.marketplace.dto;

import com.carservice.backend.marketplace.enums.WalletReferenceType;
import com.carservice.backend.marketplace.enums.WalletTransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletTransactionResponse {

    private Long id;
    private Long walletId;
    private Long workshopId;
    private WalletTransactionType type;
    private WalletReferenceType referenceType;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String referenceId;
    private String idempotencyKey;
    private String description;
    private LocalDateTime createdAt;

    public WalletTransactionResponse() {
    }

    public WalletTransactionResponse(
            Long id,
            Long walletId,
            Long workshopId,
            WalletTransactionType type,
            WalletReferenceType referenceType,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String referenceId,
            String idempotencyKey,
            String description,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.walletId = walletId;
        this.workshopId = workshopId;
        this.type = type;
        this.referenceType = referenceType;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.referenceId = referenceId;
        this.idempotencyKey = idempotencyKey;
        this.description = description;
        this.createdAt = createdAt;
    }

    // Backward-compatible constructor
    public WalletTransactionResponse(
            Long id,
            Long walletId,
            WalletTransactionType type,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String referenceId,
            String description,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.walletId = walletId;
        this.type = type;
        this.referenceType = type == WalletTransactionType.DEBIT ? WalletReferenceType.OPPORTUNITY_ACCEPTANCE_FEE : WalletReferenceType.WALLET_TOPUP;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.balanceBefore = type == WalletTransactionType.DEBIT ? (balanceAfter != null ? balanceAfter.add(amount) : BigDecimal.ZERO) : (balanceAfter != null ? balanceAfter.subtract(amount).max(BigDecimal.ZERO) : BigDecimal.ZERO);
        this.referenceId = referenceId;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public Long getWorkshopId() {
        return workshopId;
    }

    public void setWorkshopId(Long workshopId) {
        this.workshopId = workshopId;
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

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
