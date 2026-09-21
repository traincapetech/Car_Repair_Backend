package com.carservice.backend.admin.dto;

public class AdminUserSummaryResponse {

    private long totalUsers;
    private long activeUsers;
    private long suspendedUsers;
    private long pendingUsers;
    private long inactiveUsers;
    private long adminUsers;
    private long partnerUsers;
    private long customerUsers;

    public AdminUserSummaryResponse() {
    }

    public AdminUserSummaryResponse(
            long totalUsers,
            long activeUsers,
            long suspendedUsers,
            long pendingUsers,
            long inactiveUsers,
            long adminUsers,
            long partnerUsers,
            long customerUsers
    ) {
        this.totalUsers = totalUsers;
        this.activeUsers = activeUsers;
        this.suspendedUsers = suspendedUsers;
        this.pendingUsers = pendingUsers;
        this.inactiveUsers = inactiveUsers;
        this.adminUsers = adminUsers;
        this.partnerUsers = partnerUsers;
        this.customerUsers = customerUsers;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public long getSuspendedUsers() {
        return suspendedUsers;
    }

    public void setSuspendedUsers(long suspendedUsers) {
        this.suspendedUsers = suspendedUsers;
    }

    public long getPendingUsers() {
        return pendingUsers;
    }

    public void setPendingUsers(long pendingUsers) {
        this.pendingUsers = pendingUsers;
    }

    public long getInactiveUsers() {
        return inactiveUsers;
    }

    public void setInactiveUsers(long inactiveUsers) {
        this.inactiveUsers = inactiveUsers;
    }

    public long getAdminUsers() {
        return adminUsers;
    }

    public void setAdminUsers(long adminUsers) {
        this.adminUsers = adminUsers;
    }

    public long getPartnerUsers() {
        return partnerUsers;
    }

    public void setPartnerUsers(long partnerUsers) {
        this.partnerUsers = partnerUsers;
    }

    public long getCustomerUsers() {
        return customerUsers;
    }

    public void setCustomerUsers(long customerUsers) {
        this.customerUsers = customerUsers;
    }
}
