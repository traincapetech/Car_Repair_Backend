package com.carservice.backend.admin.dto;

public class AdminWorkshopSummaryResponse {
    private long totalWorkshops;
    private long activeWorkshops;
    private long pendingVerificationWorkshops;
    private long suspendedOrInactiveWorkshops;

    public AdminWorkshopSummaryResponse() {
    }

    public AdminWorkshopSummaryResponse(long totalWorkshops, long activeWorkshops, long pendingVerificationWorkshops, long suspendedOrInactiveWorkshops) {
        this.totalWorkshops = totalWorkshops;
        this.activeWorkshops = activeWorkshops;
        this.pendingVerificationWorkshops = pendingVerificationWorkshops;
        this.suspendedOrInactiveWorkshops = suspendedOrInactiveWorkshops;
    }

    public long getTotalWorkshops() {
        return totalWorkshops;
    }

    public void setTotalWorkshops(long totalWorkshops) {
        this.totalWorkshops = totalWorkshops;
    }

    public long getActiveWorkshops() {
        return activeWorkshops;
    }

    public void setActiveWorkshops(long activeWorkshops) {
        this.activeWorkshops = activeWorkshops;
    }

    public long getPendingVerificationWorkshops() {
        return pendingVerificationWorkshops;
    }

    public void setPendingVerificationWorkshops(long pendingVerificationWorkshops) {
        this.pendingVerificationWorkshops = pendingVerificationWorkshops;
    }

    public long getSuspendedOrInactiveWorkshops() {
        return suspendedOrInactiveWorkshops;
    }

    public void setSuspendedOrInactiveWorkshops(long suspendedOrInactiveWorkshops) {
        this.suspendedOrInactiveWorkshops = suspendedOrInactiveWorkshops;
    }
}
