package com.carservice.backend.admin.dto;

import com.carservice.backend.user.enums.UserRole;
import java.time.LocalDateTime;

/**
 * Safe, sanitized administrative profile response.
 * Strictly omits passwords, hashes, refresh tokens, JWTs, and internal security credentials.
 */
public class AdminProfileResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private UserRole role;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public AdminProfileResponse() {
    }

    public AdminProfileResponse(
            Long id,
            String name,
            String email,
            String phone,
            UserRole role,
            Boolean isActive,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.isActive = isActive;
        this.createdAt = createdAt;
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
}
