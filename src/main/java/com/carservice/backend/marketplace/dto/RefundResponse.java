package com.carservice.backend.marketplace.dto;

import com.carservice.backend.marketplace.enums.RefundReason;
import com.carservice.backend.marketplace.enums.RefundStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RefundResponse {

    private Long id;
    private Long paymentId;
    private Long opportunityId;
    private Long workshopId;
    private String workshopName;
    private BigDecimal refundAmount;
    private RefundStatus refundStatus;
    private RefundReason refundReason;
    private String razorpayRefundId;
    private String failureReason;
    private LocalDateTime initiatedAt;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;

    public RefundResponse() {
    }

    public RefundResponse(
            Long id,
            Long paymentId,
            Long opportunityId,
            Long workshopId,
            String workshopName,
            BigDecimal refundAmount,
            RefundStatus refundStatus,
            RefundReason refundReason,
            String razorpayRefundId,
            String failureReason,
            LocalDateTime initiatedAt,
            LocalDateTime processedAt,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.paymentId = paymentId;
        this.opportunityId = opportunityId;
        this.workshopId = workshopId;
        this.workshopName = workshopName;
        this.refundAmount = refundAmount;
        this.refundStatus = refundStatus;
        this.refundReason = refundReason;
        this.razorpayRefundId = razorpayRefundId;
        this.failureReason = failureReason;
        this.initiatedAt = initiatedAt;
        this.processedAt = processedAt;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getOpportunityId() {
        return opportunityId;
    }

    public void setOpportunityId(Long opportunityId) {
        this.opportunityId = opportunityId;
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

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
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

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
