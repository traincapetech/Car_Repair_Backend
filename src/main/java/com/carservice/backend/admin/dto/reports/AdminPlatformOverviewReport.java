package com.carservice.backend.admin.dto.reports;

public class AdminPlatformOverviewReport {

    private long totalCustomers;
    private long totalWorkshops;
    private long verifiedWorkshops;
    private long pendingWorkshopApprovals;
    private long activeServiceCatalogItems;
    private long totalServiceRequests;
    private long totalBookings;
    private long completedBookings;
    private long cancelledBookings;

    // Period-filtered counters (equals lifetime if no filter applied)
    private long periodCustomers;
    private long periodWorkshops;
    private long periodServiceRequests;
    private long periodBookings;
    private long periodCompletedBookings;
    private long periodCancelledBookings;

    public AdminPlatformOverviewReport() {}

    public long getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(long totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public long getTotalWorkshops() {
        return totalWorkshops;
    }

    public void setTotalWorkshops(long totalWorkshops) {
        this.totalWorkshops = totalWorkshops;
    }

    public long getVerifiedWorkshops() {
        return verifiedWorkshops;
    }

    public void setVerifiedWorkshops(long verifiedWorkshops) {
        this.verifiedWorkshops = verifiedWorkshops;
    }

    public long getPendingWorkshopApprovals() {
        return pendingWorkshopApprovals;
    }

    public void setPendingWorkshopApprovals(long pendingWorkshopApprovals) {
        this.pendingWorkshopApprovals = pendingWorkshopApprovals;
    }

    public long getActiveServiceCatalogItems() {
        return activeServiceCatalogItems;
    }

    public void setActiveServiceCatalogItems(long activeServiceCatalogItems) {
        this.activeServiceCatalogItems = activeServiceCatalogItems;
    }

    public long getTotalServiceRequests() {
        return totalServiceRequests;
    }

    public void setTotalServiceRequests(long totalServiceRequests) {
        this.totalServiceRequests = totalServiceRequests;
    }

    public long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(long totalBookings) {
        this.totalBookings = totalBookings;
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

    public long getPeriodCustomers() {
        return periodCustomers;
    }

    public void setPeriodCustomers(long periodCustomers) {
        this.periodCustomers = periodCustomers;
    }

    public long getPeriodWorkshops() {
        return periodWorkshops;
    }

    public void setPeriodWorkshops(long periodWorkshops) {
        this.periodWorkshops = periodWorkshops;
    }

    public long getPeriodServiceRequests() {
        return periodServiceRequests;
    }

    public void setPeriodServiceRequests(long periodServiceRequests) {
        this.periodServiceRequests = periodServiceRequests;
    }

    public long getPeriodBookings() {
        return periodBookings;
    }

    public void setPeriodBookings(long periodBookings) {
        this.periodBookings = periodBookings;
    }

    public long getPeriodCompletedBookings() {
        return periodCompletedBookings;
    }

    public void setPeriodCompletedBookings(long periodCompletedBookings) {
        this.periodCompletedBookings = periodCompletedBookings;
    }

    public long getPeriodCancelledBookings() {
        return periodCancelledBookings;
    }

    public void setPeriodCancelledBookings(long periodCancelledBookings) {
        this.periodCancelledBookings = periodCancelledBookings;
    }
}
