package com.carservice.backend.admin.dto;

public class WorkshopActionReasonRequest {

    private String reason;

    public WorkshopActionReasonRequest() {
    }

    public WorkshopActionReasonRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
