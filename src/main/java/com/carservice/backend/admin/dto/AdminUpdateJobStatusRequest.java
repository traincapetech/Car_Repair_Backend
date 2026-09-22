package com.carservice.backend.admin.dto;

import com.carservice.backend.marketplace.enums.WorkshopJobStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AdminUpdateJobStatusRequest {

    @NotNull(message = "Job status is required")
    private WorkshopJobStatus status;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;

    public AdminUpdateJobStatusRequest() {
    }

    public AdminUpdateJobStatusRequest(WorkshopJobStatus status, String notes, String reason) {
        this.status = status;
        this.notes = notes;
        this.reason = reason;
    }

    public WorkshopJobStatus getStatus() {
        return status;
    }

    public void setStatus(WorkshopJobStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
