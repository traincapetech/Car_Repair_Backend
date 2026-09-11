package com.carservice.backend.marketplace.entity;

import com.carservice.backend.marketplace.enums.LeadPaymentMethod;
import com.carservice.backend.marketplace.enums.LeadPaymentStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "lead_payments",
        indexes = {
                @Index(name = "idx_lp_opportunity_id", columnList = "opportunity_id"),
                @Index(name = "idx_lp_tx_ref", columnList = "transaction_reference")
        }
)
public class LeadPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opportunity_id", nullable = false, unique = true)
    private LeadOpportunity opportunity;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private LeadPaymentMethod paymentMethod;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_reference", nullable = false, unique = true, length = 64)
    private String transactionReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeadPaymentStatus status = LeadPaymentStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public LeadPayment() {
    }

    public LeadPayment(
            LeadOpportunity opportunity,
            LeadPaymentMethod paymentMethod,
            BigDecimal amount,
            String transactionReference,
            LeadPaymentStatus status
    ) {
        this.opportunity = opportunity;
        this.paymentMethod = paymentMethod;
        this.amount = amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        this.transactionReference = transactionReference;
        this.status = status != null ? status : LeadPaymentStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = LeadPaymentStatus.PENDING;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LeadOpportunity getOpportunity() {
        return opportunity;
    }

    public void setOpportunity(LeadOpportunity opportunity) {
        this.opportunity = opportunity;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
