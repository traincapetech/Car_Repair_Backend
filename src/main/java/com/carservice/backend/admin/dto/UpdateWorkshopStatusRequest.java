package com.carservice.backend.admin.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateWorkshopStatusRequest {

    @NotNull(message = "isActive flag is required")
    private Boolean isActive;

    private String reason;

    public UpdateWorkshopStatusRequest() {
    }

    public UpdateWorkshopStatusRequest(Boolean isActive, String reason) {
        this.isActive = isActive;
        this.reason = reason;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
