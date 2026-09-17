package com.carservice.backend.admin.dto;

import com.carservice.backend.marketplace.enums.OpportunityStatus;
import com.carservice.backend.marketplace.enums.PaymentMethod;
import com.carservice.backend.marketplace.enums.PaymentStatus;
import com.carservice.backend.marketplace.enums.RefundReason;
import com.carservice.backend.marketplace.enums.RefundStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AdminMarketplaceOpportunityResponse {

    private Long id;
    private Long workshopId;
    private String workshopName;
    private String workshopCity;
    private String workshopPhone;

    private BigDecimal feeSnapshot;
    private OpportunityStatus status;

    private LocalDateTime viewedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime paidAt;
    private LocalDateTime unlockedAt;
    private LocalDateTime transferredAt;
    private String transferReason;

    // Payment snapshot
    private Long paymentId;
    private PaymentMethod paymentMethod;
    private BigDecimal paymentAmount;
    private PaymentStatus paymentStatus;
    private String paymentTransactionReference;
    private LocalDateTime paymentPaidAt;

    // Refund snapshot
    private Long refundId;
    private RefundStatus refundStatus;
    private BigDecimal refundAmount;
    private RefundReason refundReason;
    private LocalDateTime refundProcessedAt;

    private boolean isAssigned;
    private boolean isCustomerDetailsUnlocked;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AdminMarketplaceOpportunityResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWorkshopId() {
        return workshopId;
    }

    public void setWorkshopId(Long workshopId) {
        this.workshopId = workshopId;
    }

    public String getWorkshopName() {
        return workshopName;
    }

    public void setWorkshopName(String workshopName) {
        this.workshopName = workshopName;
    }

    public String getWorkshopCity() {
        return workshopCity;
    }

    public void setWorkshopCity(String workshopCity) {
        this.workshopCity = workshopCity;
    }

    public String getWorkshopPhone() {
        return workshopPhone;
    }

    public void setWorkshopPhone(String workshopPhone) {
        this.workshopPhone = workshopPhone;
    }

    public BigDecimal getFeeSnapshot() {
        return feeSnapshot;
    }

    public void setFeeSnapshot(BigDecimal feeSnapshot) {
        this.feeSnapshot = feeSnapshot;
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

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentTransactionReference() {
        return paymentTransactionReference;
    }

    public void setPaymentTransactionReference(String paymentTransactionReference) {
        this.paymentTransactionReference = paymentTransactionReference;
    }

    public LocalDateTime getPaymentPaidAt() {
        return paymentPaidAt;
    }

    public void setPaymentPaidAt(LocalDateTime paymentPaidAt) {
        this.paymentPaidAt = paymentPaidAt;
    }

    public Long getRefundId() {
        return refundId;
    }

    public void setRefundId(Long refundId) {
        this.refundId = refundId;
    }

    public RefundStatus getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(RefundStatus refundStatus) {
        this.refundStatus = refundStatus;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public RefundReason getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(RefundReason refundReason) {
        this.refundReason = refundReason;
    }

    public LocalDateTime getRefundProcessedAt() {
        return refundProcessedAt;
    }

    public void setRefundProcessedAt(LocalDateTime refundProcessedAt) {
        this.refundProcessedAt = refundProcessedAt;
    }

    public boolean isAssigned() {
        return isAssigned;
    }

    public void setAssigned(boolean assigned) {
        isAssigned = assigned;
    }

    public boolean isCustomerDetailsUnlocked() {
        return isCustomerDetailsUnlocked;
    }

    public void setCustomerDetailsUnlocked(boolean customerDetailsUnlocked) {
        isCustomerDetailsUnlocked = customerDetailsUnlocked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
