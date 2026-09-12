package com.carservice.backend.marketplace.dto;

import jakarta.validation.constraints.NotNull;

public class WalletClaimPaymentRequest {

    @NotNull(message = "Opportunity ID is required")
    private Long opportunityId;

    private String idempotencyKey;

    public WalletClaimPaymentRequest() {
    }

    public WalletClaimPaymentRequest(Long opportunityId, String idempotencyKey) {
        this.opportunityId = opportunityId;
        this.idempotencyKey = idempotencyKey;
    }

    public Long getOpportunityId() {
        return opportunityId;
    }

    public void setOpportunityId(Long opportunityId) {
        this.opportunityId = opportunityId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
