package com.carservice.backend.marketplace.dto;

import com.carservice.backend.marketplace.enums.LeadPaymentMethod;
import com.carservice.backend.marketplace.enums.LeadPaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LeadPaymentResponse {

    private Long id;
    private LeadPaymentMethod paymentMethod;
    private BigDecimal amount;
    private String transactionReference;
    private LeadPaymentStatus status;
    private LocalDateTime paidAt;

    public LeadPaymentResponse() {
    }

    public LeadPaymentResponse(Long id, LeadPaymentMethod paymentMethod, BigDecimal amount, String transactionReference, LeadPaymentStatus status, LocalDateTime paidAt) {
        this.id = id;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.transactionReference = transactionReference;
        this.status = status;
        this.paidAt = paidAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LeadPaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(LeadPaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public LeadPaymentStatus getStatus() {
        return status;
    }

    public void setStatus(LeadPaymentStatus status) {
        this.status = status;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}
