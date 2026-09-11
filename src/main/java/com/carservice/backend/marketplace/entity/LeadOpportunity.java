package com.carservice.backend.marketplace.entity;

import com.carservice.backend.marketplace.enums.OpportunityStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "lead_opportunities",
        indexes = {
                @Index(name = "idx_lo_request_id", columnList = "service_request_id"),
                @Index(name = "idx_lo_workshop_id", columnList = "workshop_id"),
                @Index(name = "idx_lo_status", columnList = "status"),
                @Index(name = "idx_lo_workshop_status", columnList = "workshop_id, status")
        }
)
public class LeadOpportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_request_id", nullable = false)
    private ServiceRequest serviceRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workshop_id", nullable = false)
    private Workshop workshop;

    @Column(name = "fee_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal feeSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OpportunityStatus status = OpportunityStatus.AVAILABLE;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;

    @Column(name = "transferred_at")
    private LocalDateTime transferredAt;

    @Column(name = "transfer_reason", length = 255)
    private String transferReason;

    @OneToOne(mappedBy = "opportunity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private LeadPayment payment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public LeadOpportunity() {
    }

    public LeadOpportunity(ServiceRequest serviceRequest, Workshop workshop, BigDecimal feeSnapshot) {
        this.serviceRequest = serviceRequest;
        this.workshop = workshop;
        this.feeSnapshot = feeSnapshot != null ? feeSnapshot.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        this.status = OpportunityStatus.AVAILABLE;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = OpportunityStatus.AVAILABLE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isCustomerDetailsUnlocked() {
        return this.status == OpportunityStatus.PAID
                || this.status == OpportunityStatus.CUSTOMER_DETAILS_UNLOCKED
                || this.status == OpportunityStatus.ASSIGNED;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ServiceRequest getServiceRequest() {
        return serviceRequest;
    }

    public void setServiceRequest(ServiceRequest serviceRequest) {
        this.serviceRequest = serviceRequest;
    }

    public Workshop getWorkshop() {
        return workshop;
    }

    public void setWorkshop(Workshop workshop) {
        this.workshop = workshop;
    }

    public BigDecimal getFeeSnapshot() {
        return feeSnapshot;
    }

    public void setFeeSnapshot(BigDecimal feeSnapshot) {
        this.feeSnapshot = feeSnapshot != null ? feeSnapshot.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    public OpportunityStatus getStatus() {
        return status;
    }

    public void setStatus(OpportunityStatus status) {
        this.status = status;
    }

    public LocalDateTime getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(LocalDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getUnlockedAt() {
        return unlockedAt;
    }

    public void setUnlockedAt(LocalDateTime unlockedAt) {
        this.unlockedAt = unlockedAt;
    }

    public LocalDateTime getTransferredAt() {
        return transferredAt;
    }

    public void setTransferredAt(LocalDateTime transferredAt) {
        this.transferredAt = transferredAt;
    }

    public String getTransferReason() {
        return transferReason;
    }

    public void setTransferReason(String transferReason) {
        this.transferReason = transferReason;
    }

    public LeadPayment getPayment() {
        return payment;
    }

    public void setPayment(LeadPayment payment) {
        this.payment = payment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
