package com.carservice.backend.admin.dto;

import com.carservice.backend.user.enums.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateUserStatusRequest {

    @NotNull(message = "Status is required")
    private UserStatus status;

    @NotBlank(message = "Reason is required for status mutations")
    @Size(max = 255, message = "Reason cannot exceed 255 characters")
    private String reason;

    public UpdateUserStatusRequest() {
    }

    public UpdateUserStatusRequest(UserStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
