package com.carservice.backend.admin.dto;

import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.user.enums.UserStatus;

import java.time.LocalDateTime;

public class AdminUserListResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private UserRole role;
    private UserStatus status;
    private Boolean isActive;
    private Boolean hasWorkshop;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AdminUserListResponse() {
    }

    public AdminUserListResponse(
            Long id,
            String name,
            String email,
            String phone,
            UserRole role,
            UserStatus status,
            Boolean isActive,
            Boolean hasWorkshop,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.isActive = isActive;
        this.hasWorkshop = hasWorkshop;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public Boolean getHasWorkshop() {
        return hasWorkshop;
    }

    public void setHasWorkshop(Boolean hasWorkshop) {
        this.hasWorkshop = hasWorkshop;
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
}
