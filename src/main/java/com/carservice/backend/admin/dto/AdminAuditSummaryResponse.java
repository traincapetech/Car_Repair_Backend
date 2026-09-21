package com.carservice.backend.admin.dto;

import java.util.List;

public class AdminAuditSummaryResponse {

    private long totalEvents;
    private long todayEvents;
    private long failedEvents;
    private long securityEvents;
    private List<AdminAuditLogResponse> recentEvents;

    public AdminAuditSummaryResponse() {
    }

    public AdminAuditSummaryResponse(long totalEvents, long todayEvents, long failedEvents, long securityEvents, List<AdminAuditLogResponse> recentEvents) {
        this.totalEvents = totalEvents;
        this.todayEvents = todayEvents;
        this.failedEvents = failedEvents;
        this.securityEvents = securityEvents;
        this.recentEvents = recentEvents;
    }

    public long getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(long totalEvents) {
        this.totalEvents = totalEvents;
    }

    public long getTodayEvents() {
        return todayEvents;
    }

    public void setTodayEvents(long todayEvents) {
        this.todayEvents = todayEvents;
    }

    public long getFailedEvents() {
        return failedEvents;
    }

    public void setFailedEvents(long failedEvents) {
        this.failedEvents = failedEvents;
    }

    public long getSecurityEvents() {
        return securityEvents;
    }

    public void setSecurityEvents(long securityEvents) {
        this.securityEvents = securityEvents;
    }

    public List<AdminAuditLogResponse> getRecentEvents() {
        return recentEvents;
    }

    public void setRecentEvents(List<AdminAuditLogResponse> recentEvents) {
        this.recentEvents = recentEvents;
    }
}
