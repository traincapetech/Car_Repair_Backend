package com.carservice.backend.marketplace.dto;

import com.carservice.backend.marketplace.enums.LeadPaymentMethod;
import jakarta.validation.constraints.NotNull;

public class DirectPaymentRequest {

    @NotNull(message = "Payment method is required")
    private LeadPaymentMethod paymentMethod;

    private String transactionReference;

    public DirectPaymentRequest() {
    }

    public DirectPaymentRequest(LeadPaymentMethod paymentMethod, String transactionReference) {
        this.paymentMethod = paymentMethod;
        this.transactionReference = transactionReference;
    }

    public LeadPaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(LeadPaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }
}
