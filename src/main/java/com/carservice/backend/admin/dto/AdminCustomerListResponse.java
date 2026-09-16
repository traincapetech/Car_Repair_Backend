package com.carservice.backend.admin.dto;

import com.carservice.backend.user.enums.UserRole;

import java.time.LocalDateTime;

public class AdminCustomerListResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private UserRole role;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long vehicleCount;
    private long bookingCount;
    private long serviceRequestCount;

    public AdminCustomerListResponse() {
    }

    public AdminCustomerListResponse(
            Long id,
            String name,
            String email,
            String phone,
            UserRole role,
            Boolean isActive,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            long vehicleCount,
            long bookingCount,
            long serviceRequestCount
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.vehicleCount = vehicleCount;
        this.bookingCount = bookingCount;
        this.serviceRequestCount = serviceRequestCount;
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

    public long getVehicleCount() {
        return vehicleCount;
    }

    public void setVehicleCount(long vehicleCount) {
        this.vehicleCount = vehicleCount;
    }

    public long getBookingCount() {
        return bookingCount;
    }

    public void setBookingCount(long bookingCount) {
        this.bookingCount = bookingCount;
    }

    public long getServiceRequestCount() {
        return serviceRequestCount;
    }

    public void setServiceRequestCount(long serviceRequestCount) {
        this.serviceRequestCount = serviceRequestCount;
    }
}
