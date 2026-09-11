package com.carservice.backend.marketplace.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class UpdateLeadFeeRequest {

    @NotNull(message = "Fee is required")
    @DecimalMin(value = "0.00", message = "Fee cannot be negative")
    private BigDecimal fee;

    public UpdateLeadFeeRequest() {
    }

    public UpdateLeadFeeRequest(BigDecimal fee) {
        this.fee = fee;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }
}
