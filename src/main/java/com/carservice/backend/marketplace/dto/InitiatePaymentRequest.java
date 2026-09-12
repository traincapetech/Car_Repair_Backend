package com.carservice.backend.marketplace.dto;

import com.carservice.backend.marketplace.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public class InitiatePaymentRequest {

    @NotNull(message = "Opportunity ID is required")
    private Long opportunityId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String idempotencyKey;

    public InitiatePaymentRequest() {
    }

    public InitiatePaymentRequest(Long opportunityId, PaymentMethod paymentMethod, String idempotencyKey) {
        this.opportunityId = opportunityId;
        this.paymentMethod = paymentMethod;
        this.idempotencyKey = idempotencyKey;
    }

    public Long getOpportunityId() {
        return opportunityId;
    }

    public void setOpportunityId(Long opportunityId) {
        this.opportunityId = opportunityId;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
