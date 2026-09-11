package com.carservice.backend.marketplace.entity;

import com.carservice.backend.marketplace.enums.MarketplaceEventType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "marketplace_audit_events",
        indexes = {
                @Index(name = "idx_mae_request_id", columnList = "service_request_id"),
                @Index(name = "idx_mae_opportunity_id", columnList = "lead_opportunity_id"),
                @Index(name = "idx_mae_workshop_id", columnList = "workshop_id"),
                @Index(name = "idx_mae_event_type", columnList = "event_type"),
                @Index(name = "idx_mae_created_at", columnList = "created_at")
        }
)
public class MarketplaceAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private MarketplaceEventType eventType;

    @Column(name = "service_request_id")
    private Long serviceRequestId;

    @Column(name = "lead_opportunity_id")
    private Long leadOpportunityId;

    @Column(name = "workshop_id")
    private Long workshopId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public MarketplaceAuditEvent() {
    }

    public MarketplaceAuditEvent(
            MarketplaceEventType eventType,
            Long serviceRequestId,
            Long leadOpportunityId,
            Long workshopId,
            Long actorUserId,
            String description,
            String metadataJson
    ) {
        this.eventType = eventType;
        this.serviceRequestId = serviceRequestId;
        this.leadOpportunityId = leadOpportunityId;
        this.workshopId = workshopId;
        this.actorUserId = actorUserId;
        this.description = description;
        this.metadataJson = metadataJson;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
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

    public Long getServiceRequestId() {
        return serviceRequestId;
    }

    public void setServiceRequestId(Long serviceRequestId) {
        this.serviceRequestId = serviceRequestId;
    }

    public Long getLeadOpportunityId() {
        return leadOpportunityId;
    }

    public void setLeadOpportunityId(Long leadOpportunityId) {
        this.leadOpportunityId = leadOpportunityId;
    }

    public Long getWorkshopId() {
        return workshopId;
    }

    public void setWorkshopId(Long workshopId) {
        this.workshopId = workshopId;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
}
