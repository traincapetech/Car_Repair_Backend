package com.carservice.backend.admin.dto;

import com.carservice.backend.user.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateUserRoleRequest {

    @NotNull(message = "Role is required")
    private UserRole role;

    @NotBlank(message = "Reason is required for role assignment mutations")
    @Size(max = 255, message = "Reason cannot exceed 255 characters")
    private String reason;

    public UpdateUserRoleRequest() {
    }

    public UpdateUserRoleRequest(UserRole role, String reason) {
        this.role = role;
        this.reason = reason;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
