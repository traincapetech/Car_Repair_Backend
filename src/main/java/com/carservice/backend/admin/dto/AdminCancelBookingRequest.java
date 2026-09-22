package com.carservice.backend.admin.dto;

import jakarta.validation.constraints.Size;

public class AdminCancelBookingRequest {

    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;

    public AdminCancelBookingRequest() {
    }

    public AdminCancelBookingRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
