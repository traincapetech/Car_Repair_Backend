package com.carservice.backend.admin.dto.reports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminBookingReport {

    private long totalBookings;
    private long pending;
    private long confirmed;
    private long inProgress;
    private long completed;
    private long cancelled;

    private double averageBookingsPerDay;
    private Map<String, Double> statusDistribution = new HashMap<>();
    private List<AdminReportTimelinePoint> timeline = new ArrayList<>();

    public AdminBookingReport() {}

    public long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(long totalBookings) {
        this.totalBookings = totalBookings;
    }

    public long getPending() {
        return pending;
    }

    public void setPending(long pending) {
        this.pending = pending;
    }

    public long getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(long confirmed) {
        this.confirmed = confirmed;
    }

    public long getInProgress() {
        return inProgress;
    }

    public void setInProgress(long inProgress) {
        this.inProgress = inProgress;
    }

    public long getCompleted() {
        return completed;
    }

    public void setCompleted(long completed) {
        this.completed = completed;
    }

    public long getCancelled() {
        return cancelled;
    }

    public void setCancelled(long cancelled) {
        this.cancelled = cancelled;
    }

    public double getAverageBookingsPerDay() {
        return averageBookingsPerDay;
    }

    public void setAverageBookingsPerDay(double averageBookingsPerDay) {
        this.averageBookingsPerDay = averageBookingsPerDay;
    }

    public Map<String, Double> getStatusDistribution() {
        return statusDistribution;
    }

    public void setStatusDistribution(Map<String, Double> statusDistribution) {
        this.statusDistribution = statusDistribution;
    }

    public List<AdminReportTimelinePoint> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<AdminReportTimelinePoint> timeline) {
        this.timeline = timeline;
    }
}
