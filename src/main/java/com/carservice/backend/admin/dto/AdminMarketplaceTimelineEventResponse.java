package com.carservice.backend.admin.dto;

import com.carservice.backend.marketplace.enums.MarketplaceEventType;

import java.time.LocalDateTime;

public class AdminMarketplaceTimelineEventResponse {

    private Long id;
    private MarketplaceEventType eventType;
    private String description;
    private Long actorUserId;
    private Long workshopId;
    private String workshopName;
    private Long leadOpportunityId;
    private String metadataJson;
    private LocalDateTime createdAt;

    public AdminMarketplaceTimelineEventResponse() {
    }

    public AdminMarketplaceTimelineEventResponse(
            Long id,
            MarketplaceEventType eventType,
            String description,
            Long actorUserId,
            Long workshopId,
            String workshopName,
            Long leadOpportunityId,
            String metadataJson,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.eventType = eventType;
        this.description = description;
        this.actorUserId = actorUserId;
        this.workshopId = workshopId;
        this.workshopName = workshopName;
        this.leadOpportunityId = leadOpportunityId;
        this.metadataJson = metadataJson;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MarketplaceEventType getEventType() {
        return eventType;
    }

    public void setEventType(MarketplaceEventType eventType) {
        this.eventType = eventType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public Long getWorkshopId() {
        return workshopId;
    }

    public void setWorkshopId(Long workshopId) {
        this.workshopId = workshopId;
    }

    public String getWorkshopName() {
        return workshopName;
    }

    public void setWorkshopName(String workshopName) {
        this.workshopName = workshopName;
    }

    public Long getLeadOpportunityId() {
        return leadOpportunityId;
    }

    public void setLeadOpportunityId(Long leadOpportunityId) {
        this.leadOpportunityId = leadOpportunityId;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
