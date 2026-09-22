package com.carservice.backend.admin.dto;

import java.math.BigDecimal;

public class AdminBookingSummaryResponse {

    private long totalBookings;
    private long pendingBookings;
    private long confirmedBookings;
    private long inProgressBookings;
    private long completedBookings;
    private long cancelledBookings;
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    public AdminBookingSummaryResponse() {
    }

    public AdminBookingSummaryResponse(long totalBookings, long pendingBookings, long confirmedBookings,
                                       long inProgressBookings, long completedBookings, long cancelledBookings,
                                       BigDecimal totalRevenue) {
        this.totalBookings = totalBookings;
        this.pendingBookings = pendingBookings;
        this.confirmedBookings = confirmedBookings;
        this.inProgressBookings = inProgressBookings;
        this.completedBookings = completedBookings;
        this.cancelledBookings = cancelledBookings;
        this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
    }

    public long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(long totalBookings) {
        this.totalBookings = totalBookings;
    }

    public long getPendingBookings() {
        return pendingBookings;
    }

    public void setPendingBookings(long pendingBookings) {
        this.pendingBookings = pendingBookings;
    }

    public long getConfirmedBookings() {
        return confirmedBookings;
    }

    public void setConfirmedBookings(long confirmedBookings) {
        this.confirmedBookings = confirmedBookings;
    }

    public long getInProgressBookings() {
        return inProgressBookings;
    }

    public void setInProgressBookings(long inProgressBookings) {
        this.inProgressBookings = inProgressBookings;
    }

    public long getCompletedBookings() {
        return completedBookings;
    }

    public void setCompletedBookings(long completedBookings) {
        this.completedBookings = completedBookings;
    }

    public long getCancelledBookings() {
        return cancelledBookings;
    }

    public void setCancelledBookings(long cancelledBookings) {
        this.cancelledBookings = cancelledBookings;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
    }
}
