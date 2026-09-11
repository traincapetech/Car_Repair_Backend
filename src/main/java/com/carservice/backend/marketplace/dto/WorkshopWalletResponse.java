package com.carservice.backend.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WorkshopWalletResponse {

    private Long id;
    private Long workshopId;
    private String workshopName;
    private BigDecimal balance;
    private LocalDateTime updatedAt;

    public WorkshopWalletResponse() {
    }

    public WorkshopWalletResponse(Long id, Long workshopId, String workshopName, BigDecimal balance, LocalDateTime updatedAt) {
        this.id = id;
        this.workshopId = workshopId;
        this.workshopName = workshopName;
        this.balance = balance;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
