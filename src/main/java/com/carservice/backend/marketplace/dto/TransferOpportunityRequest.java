package com.carservice.backend.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TransferOpportunityRequest {

    @NotBlank(message = "Transfer reason is required")
    @Size(max = 255, message = "Transfer reason must not exceed 255 characters")
    private String reason;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    public TransferOpportunityRequest() {
    }

    public TransferOpportunityRequest(String reason) {
        this.reason = reason;
    }

    public TransferOpportunityRequest(String reason, String notes) {
        this.reason = reason;
        this.notes = notes;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

