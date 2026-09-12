package com.carservice.backend.marketplace.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class TopupInitiateRequest {

    @NotNull(message = "Top-up amount is required")
    @DecimalMin(value = "100.00", message = "Minimum top-up amount is ₹100.00")
    @DecimalMax(value = "50000.00", message = "Maximum top-up amount per transaction is ₹50,000.00")
    private BigDecimal amount;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    public TopupInitiateRequest() {
    }

    public TopupInitiateRequest(BigDecimal amount, String description) {
        this.amount = amount;
        this.description = description;
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
}
