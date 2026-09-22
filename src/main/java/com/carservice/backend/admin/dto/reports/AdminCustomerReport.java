package com.carservice.backend.admin.dto.reports;

import java.util.ArrayList;
import java.util.List;

public class AdminCustomerReport {

    private long totalCustomers;
    private long activeCustomers;
    private long inactiveSuspendedCustomers;
    private long newCustomers;
    private double growthRatePercentage;
    private List<AdminReportTimelinePoint> timeline = new ArrayList<>();

    public AdminCustomerReport() {}

    public long getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(long totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public long getActiveCustomers() {
        return activeCustomers;
    }

    public void setActiveCustomers(long activeCustomers) {
        this.activeCustomers = activeCustomers;
    }

    public long getInactiveSuspendedCustomers() {
        return inactiveSuspendedCustomers;
    }

    public void setInactiveSuspendedCustomers(long inactiveSuspendedCustomers) {
        this.inactiveSuspendedCustomers = inactiveSuspendedCustomers;
    }

    public long getNewCustomers() {
        return newCustomers;
    }

    public void setNewCustomers(long newCustomers) {
        this.newCustomers = newCustomers;
    }

    public double getGrowthRatePercentage() {
        return growthRatePercentage;
    }

    public void setGrowthRatePercentage(double growthRatePercentage) {
        this.growthRatePercentage = growthRatePercentage;
    }

    public List<AdminReportTimelinePoint> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<AdminReportTimelinePoint> timeline) {
        this.timeline = timeline;
    }
}
