package com.carservice.backend.marketplace.dto;

import com.carservice.backend.marketplace.enums.WalletStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WorkshopWalletResponse {

    private Long id;
    private Long workshopId;
    private String workshopName;
    private BigDecimal balance;
    private String currency = "INR";
    private WalletStatus status = WalletStatus.ACTIVE;
    private LocalDateTime updatedAt;

    public WorkshopWalletResponse() {
    }

    public WorkshopWalletResponse(Long id, Long workshopId, String workshopName, BigDecimal balance, LocalDateTime updatedAt) {
        this.id = id;
        this.workshopId = workshopId;
        this.workshopName = workshopName;
        this.balance = balance;
        this.currency = "INR";
        this.status = WalletStatus.ACTIVE;
        this.updatedAt = updatedAt;
    }

    public WorkshopWalletResponse(Long id, Long workshopId, String workshopName, BigDecimal balance, String currency, WalletStatus status, LocalDateTime updatedAt) {
        this.id = id;
        this.workshopId = workshopId;
        this.workshopName = workshopName;
        this.balance = balance;
        this.currency = currency != null ? currency : "INR";
        this.status = status != null ? status : WalletStatus.ACTIVE;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWorkshopId() {
        return workshopId;
    }

    public void setWorkshopId(Long workshopId) {
        this.workshopId = workshopId;
    }

    public String getWorkshopName() {
        return workshopName;
    }

    public void setWorkshopName(String workshopName) {
        this.workshopName = workshopName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public WalletStatus getStatus() {
        return status;
    }

    public void setStatus(WalletStatus status) {
        this.status = status;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
