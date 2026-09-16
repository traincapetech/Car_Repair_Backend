package com.carservice.backend.admin.dto;

import com.carservice.backend.user.enums.UserRole;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdminCustomerDetailResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private UserRole role;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Vehicle stats
    private long totalVehicles;
    private List<AdminCustomerVehicleResponse> vehicles = new ArrayList<>();

    // Booking stats
    private long totalBookings;
    private long pendingBookings;
    private long confirmedBookings;
    private long inProgressBookings;
    private long completedBookings;
    private long cancelledBookings;

    // Service request stats
    private long totalServiceRequests;
    private long submittedRequests;
    private long matchedRequests;
    private long inProgressRequests;
    private long completedRequests;
    private long cancelledRequests;

    public AdminCustomerDetailResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getTotalVehicles() {
        return totalVehicles;
    }

    public void setTotalVehicles(long totalVehicles) {
        this.totalVehicles = totalVehicles;
    }

    public List<AdminCustomerVehicleResponse> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<AdminCustomerVehicleResponse> vehicles) {
        this.vehicles = vehicles;
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

    public long getTotalServiceRequests() {
        return totalServiceRequests;
    }

    public void setTotalServiceRequests(long totalServiceRequests) {
        this.totalServiceRequests = totalServiceRequests;
    }

    public long getSubmittedRequests() {
        return submittedRequests;
    }

    public void setSubmittedRequests(long submittedRequests) {
        this.submittedRequests = submittedRequests;
    }

    public long getMatchedRequests() {
        return matchedRequests;
    }

    public void setMatchedRequests(long matchedRequests) {
        this.matchedRequests = matchedRequests;
    }

    public long getInProgressRequests() {
        return inProgressRequests;
    }

    public void setInProgressRequests(long inProgressRequests) {
        this.inProgressRequests = inProgressRequests;
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
}
