package com.carservice.backend.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TopupInitiateResponse {

    private String topupIntentId;
    private Long workshopId;
    private String workshopName;
    private BigDecimal amount;
    private String currency;
    private String status; // PENDING
    private String description;
    private LocalDateTime createdAt;

    public TopupInitiateResponse() {
    }

    public TopupInitiateResponse(
            String topupIntentId,
            Long workshopId,
            String workshopName,
            BigDecimal amount,
            String currency,
            String status,
            String description,
            LocalDateTime createdAt
    ) {
        this.topupIntentId = topupIntentId;
        this.workshopId = workshopId;
        this.workshopName = workshopName;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.description = description;
        this.createdAt = createdAt;
    }

    public String getTopupIntentId() {
        return topupIntentId;
    }

    public String getIntentId() {
        return topupIntentId;
    }

    public void setTopupIntentId(String topupIntentId) {
        this.topupIntentId = topupIntentId;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
