package com.carservice.backend.marketplace.event;

import java.time.LocalDateTime;

public class OpportunityTransferredEvent {

    private final Long opportunityId;
    private final Long serviceRequestId;
    private final Long workshopId;
    private final String reason;
    private final LocalDateTime timestamp;

    public OpportunityTransferredEvent(Long opportunityId, Long serviceRequestId, Long workshopId, String reason, LocalDateTime timestamp) {
        this.opportunityId = opportunityId;
        this.serviceRequestId = serviceRequestId;
        this.workshopId = workshopId;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    public Long getOpportunityId() {
        return opportunityId;
    }

    public Long getServiceRequestId() {
        return serviceRequestId;
    }

    public Long getWorkshopId() {
        return workshopId;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
