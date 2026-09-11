package com.carservice.backend.marketplace.dto;

import jakarta.validation.constraints.NotBlank;

public class TransferOpportunityRequest {

    @NotBlank(message = "Transfer reason is required")
    private String reason;

    public TransferOpportunityRequest() {
    }

    public TransferOpportunityRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
