package com.carservice.backend.marketplace.event;

import java.util.List;

public class OpportunityRematchedEvent {

    private final Long serviceRequestId;
    private final Long transferringWorkshopId;
    private final List<Long> newWorkshopIds;
    private final List<Long> newOpportunityIds;

    public OpportunityRematchedEvent(Long serviceRequestId, Long transferringWorkshopId, List<Long> newWorkshopIds, List<Long> newOpportunityIds) {
        this.serviceRequestId = serviceRequestId;
        this.transferringWorkshopId = transferringWorkshopId;
        this.newWorkshopIds = newWorkshopIds;
        this.newOpportunityIds = newOpportunityIds;
    }

    public Long getServiceRequestId() {
        return serviceRequestId;
    }

    public Long getTransferringWorkshopId() {
        return transferringWorkshopId;
    }

    public List<Long> getNewWorkshopIds() {
        return newWorkshopIds;
    }

    public List<Long> getNewOpportunityIds() {
        return newOpportunityIds;
    }
}
