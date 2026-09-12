package com.carservice.backend.marketplace.entity;

import com.carservice.backend.marketplace.enums.RefundReason;
import com.carservice.backend.marketplace.enums.RefundStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "refunds",
        indexes = {
                @Index(name = "idx_ref_payment_id", columnList = "payment_id"),
                @Index(name = "idx_ref_opportunity_id", columnList = "opportunity_id"),
                @Index(name = "idx_ref_workshop_id", columnList = "workshop_id"),
                @Index(name = "idx_ref_status", columnList = "refund_status")
        }
)
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private WorkshopPayment payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opportunity_id", nullable = false)
    private LeadOpportunity opportunity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workshop_id", nullable = false)
    private Workshop workshop;

    @Column(name = "refund_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", nullable = false, length = 30)
    private RefundStatus refundStatus = RefundStatus.INITIATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_reason", nullable = false, length = 50)
    private RefundReason refundReason;

    @Column(name = "razorpay_refund_id", length = 100)
    private String razorpayRefundId;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "initiated_at")
    private LocalDateTime initiatedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Refund() {
    }

    public Refund(
            WorkshopPayment payment,
            LeadOpportunity opportunity,
            Workshop workshop,
            BigDecimal refundAmount,
            RefundStatus refundStatus,
            RefundReason refundReason
    ) {
        this.payment = payment;
        this.opportunity = opportunity;
        this.workshop = workshop;
        this.refundAmount = refundAmount != null ? refundAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        this.refundStatus = refundStatus != null ? refundStatus : RefundStatus.INITIATED;
        this.refundReason = refundReason;
        this.initiatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.initiatedAt == null) {
            this.initiatedAt = now;
        }
        if (this.refundStatus == null) {
            this.refundStatus = RefundStatus.INITIATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WorkshopPayment getPayment() {
        return payment;
    }

    public void setPayment(WorkshopPayment payment) {
        this.payment = payment;
    }

    public LeadOpportunity getOpportunity() {
        return opportunity;
    }

    public void setOpportunity(LeadOpportunity opportunity) {
        this.opportunity = opportunity;
    }

    public Workshop getWorkshop() {
        return workshop;
    }

    public void setWorkshop(Workshop workshop) {
        this.workshop = workshop;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount != null ? refundAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    public RefundStatus getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(RefundStatus refundStatus) {
        this.refundStatus = refundStatus;
    }

    public RefundReason getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(RefundReason refundReason) {
        this.refundReason = refundReason;
    }

    public String getRazorpayRefundId() {
        return razorpayRefundId;
    }

    public void setRazorpayRefundId(String razorpayRefundId) {
        this.razorpayRefundId = razorpayRefundId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getInitiatedAt() {
        return initiatedAt;
    }

    public void setInitiatedAt(LocalDateTime initiatedAt) {
        this.initiatedAt = initiatedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
