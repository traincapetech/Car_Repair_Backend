package com.carservice.backend.marketplace.dto;

import com.carservice.backend.marketplace.enums.WalletTransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class AdminWalletAdjustmentRequest {

    @NotNull(message = "Workshop ID is required")
    private Long workshopId;

    @NotNull(message = "Adjustment type (CREDIT / DEBIT) is required")
    private WalletTransactionType type;

    @NotNull(message = "Adjustment amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Adjustment reason is required")
    @Size(max = 255, message = "Reason must not exceed 255 characters")
    private String reason;

    @Size(max = 100, message = "Audit reference must not exceed 100 characters")
    private String auditReference;

    public AdminWalletAdjustmentRequest() {
    }

    public AdminWalletAdjustmentRequest(Long workshopId, WalletTransactionType type, BigDecimal amount, String reason, String auditReference) {
        this.workshopId = workshopId;
        this.type = type;
        this.amount = amount;
        this.reason = reason;
        this.auditReference = auditReference;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getAuditReference() {
        return auditReference;
    }

    public void setAuditReference(String auditReference) {
        this.auditReference = auditReference;
    }
}
