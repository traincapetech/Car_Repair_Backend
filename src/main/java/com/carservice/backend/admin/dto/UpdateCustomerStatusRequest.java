package com.carservice.backend.admin.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateCustomerStatusRequest {

    @NotNull(message = "isActive is required")
    private Boolean isActive;

    public UpdateCustomerStatusRequest() {
    }

    public UpdateCustomerStatusRequest(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}
