package com.carservice.backend.admin.dto;

import java.math.BigDecimal;

public class AdminMarketplaceSummaryResponse {

    private long totalServiceRequests;
    private long activeRequests;
    private long matchingRequests;
    private long assignedRequests;
    private long completedRequests;
    private long cancelledRequests;

    private long totalOpportunities;
    private long availableOpportunities;
    private long acceptedOpportunities;
    private long transferredOpportunities;
    private long lostOpportunities;

    private long totalSuccessfulPayments;
    private BigDecimal totalAcceptanceRevenue = BigDecimal.ZERO;
    private long totalRefunds;
    private BigDecimal totalRefundedAmount = BigDecimal.ZERO;
    private long pendingRefunds;

    public AdminMarketplaceSummaryResponse() {
    }

    public long getTotalServiceRequests() {
        return totalServiceRequests;
    }

    public void setTotalServiceRequests(long totalServiceRequests) {
        this.totalServiceRequests = totalServiceRequests;
    }

    public long getActiveRequests() {
        return activeRequests;
    }

    public void setActiveRequests(long activeRequests) {
        this.activeRequests = activeRequests;
    }

    public long getMatchingRequests() {
        return matchingRequests;
    }

    public void setMatchingRequests(long matchingRequests) {
        this.matchingRequests = matchingRequests;
    }

    public long getAssignedRequests() {
        return assignedRequests;
    }

    public void setAssignedRequests(long assignedRequests) {
        this.assignedRequests = assignedRequests;
    }

    public long getCompletedRequests() {
        return completedRequests;
    }

    public void setCompletedRequests(long completedRequests) {
        this.completedRequests = completedRequests;
    }

    public long getCancelledRequests() {
        return cancelledRequests;
    }

    public void setCancelledRequests(long cancelledRequests) {
        this.cancelledRequests = cancelledRequests;
    }

    public long getTotalOpportunities() {
        return totalOpportunities;
    }

    public void setTotalOpportunities(long totalOpportunities) {
        this.totalOpportunities = totalOpportunities;
    }

    public long getAvailableOpportunities() {
        return availableOpportunities;
    }

    public void setAvailableOpportunities(long availableOpportunities) {
        this.availableOpportunities = availableOpportunities;
    }

    public long getAcceptedOpportunities() {
        return acceptedOpportunities;
    }

    public void setAcceptedOpportunities(long acceptedOpportunities) {
        this.acceptedOpportunities = acceptedOpportunities;
    }

    public long getTransferredOpportunities() {
        return transferredOpportunities;
    }

    public void setTransferredOpportunities(long transferredOpportunities) {
        this.transferredOpportunities = transferredOpportunities;
    }

    public long getLostOpportunities() {
        return lostOpportunities;
    }

    public void setLostOpportunities(long lostOpportunities) {
        this.lostOpportunities = lostOpportunities;
    }

    public long getTotalSuccessfulPayments() {
        return totalSuccessfulPayments;
    }

    public void setTotalSuccessfulPayments(long totalSuccessfulPayments) {
        this.totalSuccessfulPayments = totalSuccessfulPayments;
    }

    public BigDecimal getTotalAcceptanceRevenue() {
        return totalAcceptanceRevenue;
    }

    public void setTotalAcceptanceRevenue(BigDecimal totalAcceptanceRevenue) {
        this.totalAcceptanceRevenue = totalAcceptanceRevenue;
    }

    public long getTotalRefunds() {
        return totalRefunds;
    }

    public void setTotalRefunds(long totalRefunds) {
        this.totalRefunds = totalRefunds;
    }

    public BigDecimal getTotalRefundedAmount() {
        return totalRefundedAmount;
    }

    public void setTotalRefundedAmount(BigDecimal totalRefundedAmount) {
        this.totalRefundedAmount = totalRefundedAmount;
    }

    public long getPendingRefunds() {
        return pendingRefunds;
    }

    public void setPendingRefunds(long pendingRefunds) {
        this.pendingRefunds = pendingRefunds;
    }

    public long getTotalRequests() {
        return totalServiceRequests;
    }

    public long getRefundsCount() {
        return totalRefunds;
    }

    public BigDecimal getTotalRevenueCollected() {
        return totalAcceptanceRevenue != null ? totalAcceptanceRevenue : BigDecimal.ZERO;
    }

    public long getSuccessfulPaymentsCount() {
        return totalSuccessfulPayments;
    }
}
