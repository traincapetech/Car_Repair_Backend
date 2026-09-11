package com.carservice.backend.marketplace.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class WalletTopupRequest {

    @NotNull(message = "Top-up amount is required")
    @DecimalMin(value = "1.00", message = "Top-up amount must be at least ₹1.00")
    private BigDecimal amount;

    private String description;
    private String referenceId;

    public WalletTopupRequest() {
    }

    public WalletTopupRequest(BigDecimal amount, String description, String referenceId) {
        this.amount = amount;
        this.description = description;
        this.referenceId = referenceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }
}
