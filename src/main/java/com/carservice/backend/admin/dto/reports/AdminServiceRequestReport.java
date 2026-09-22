package com.carservice.backend.admin.dto.reports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminServiceRequestReport {

    private long totalRequests;
    private long submitted;
    private long matched;
    private long accepted;
    private long inProgress;
    private long completed;
    private long cancelled;
    private long reMatching;

    // Key-value percentages map for status distribution chart
    private Map<String, Double> statusDistribution = new HashMap<>();

    private List<AdminReportTimelinePoint> timeline = new ArrayList<>();

    public AdminServiceRequestReport() {}

    public long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public long getSubmitted() {
        return submitted;
    }

    public void setSubmitted(long submitted) {
        this.submitted = submitted;
    }

    public long getMatched() {
        return matched;
    }

    public void setMatched(long matched) {
        this.matched = matched;
    }

    public long getAccepted() {
        return accepted;
    }

    public void setAccepted(long accepted) {
        this.accepted = accepted;
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

    public long getReMatching() {
        return reMatching;
    }

    public void setReMatching(long reMatching) {
        this.reMatching = reMatching;
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
