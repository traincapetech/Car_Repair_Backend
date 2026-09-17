package com.carservice.backend.admin.dto;

import com.carservice.backend.marketplace.enums.WorkshopVerificationStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateWorkshopVerificationRequest {

    @NotNull(message = "Verification status is required")
    private WorkshopVerificationStatus status;

    private String reason;

    public UpdateWorkshopVerificationRequest() {
    }

    public UpdateWorkshopVerificationRequest(WorkshopVerificationStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public WorkshopVerificationStatus getStatus() {
        return status;
    }

    public void setStatus(WorkshopVerificationStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
