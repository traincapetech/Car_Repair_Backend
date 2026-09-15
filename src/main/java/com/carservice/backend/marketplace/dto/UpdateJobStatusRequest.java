package com.carservice.backend.marketplace.dto;

import com.carservice.backend.marketplace.enums.WorkshopJobStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateJobStatusRequest {

    @NotNull(message = "Job status is required")
    private WorkshopJobStatus status;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    public UpdateJobStatusRequest() {
    }

    public UpdateJobStatusRequest(WorkshopJobStatus status, String notes) {
        this.status = status;
        this.notes = notes;
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
}
