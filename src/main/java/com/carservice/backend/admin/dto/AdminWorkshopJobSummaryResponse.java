package com.carservice.backend.admin.dto;

public class AdminWorkshopJobSummaryResponse {

    private long totalJobs;
    private long assignedJobs;
    private long confirmedJobs;
    private long inProgressJobs;
    private long completedJobs;
    private long cancelledJobs;
    private long transferredJobs;

    public AdminWorkshopJobSummaryResponse() {
    }

    public AdminWorkshopJobSummaryResponse(long totalJobs, long assignedJobs, long confirmedJobs,
                                           long inProgressJobs, long completedJobs, long cancelledJobs,
                                           long transferredJobs) {
        this.totalJobs = totalJobs;
        this.assignedJobs = assignedJobs;
        this.confirmedJobs = confirmedJobs;
        this.inProgressJobs = inProgressJobs;
        this.completedJobs = completedJobs;
        this.cancelledJobs = cancelledJobs;
        this.transferredJobs = transferredJobs;
    }

    public long getTotalJobs() {
        return totalJobs;
    }

    public void setTotalJobs(long totalJobs) {
        this.totalJobs = totalJobs;
    }

    public long getAssignedJobs() {
        return assignedJobs;
    }

    public void setAssignedJobs(long assignedJobs) {
        this.assignedJobs = assignedJobs;
    }

    public long getConfirmedJobs() {
        return confirmedJobs;
    }

    public void setConfirmedJobs(long confirmedJobs) {
        this.confirmedJobs = confirmedJobs;
    }

    public long getInProgressJobs() {
        return inProgressJobs;
    }

    public void setInProgressJobs(long inProgressJobs) {
        this.inProgressJobs = inProgressJobs;
    }

    public long getCompletedJobs() {
        return completedJobs;
    }

    public void setCompletedJobs(long completedJobs) {
        this.completedJobs = completedJobs;
    }

    public long getCancelledJobs() {
        return cancelledJobs;
    }

    public void setCancelledJobs(long cancelledJobs) {
        this.cancelledJobs = cancelledJobs;
    }

    public long getTransferredJobs() {
        return transferredJobs;
    }

    public void setTransferredJobs(long transferredJobs) {
        this.transferredJobs = transferredJobs;
    }
}
