package com.carservice.backend.admin.dto;

import com.carservice.backend.admin.entity.AuditLog;

import java.time.LocalDateTime;

public class AdminAuditLogResponse {

    private Long id;
    private LocalDateTime createdAt;
    private Long actorUserId;
    private String actorEmail;
    private String actorName;
    private String actorRole;
    private String action;
    private String entityType;
    private String entityId;
    private String status;
    private String ipAddress;
    private String userAgent;
    private String description;

    public AdminAuditLogResponse() {
    }

    public AdminAuditLogResponse(AuditLog log) {
        if (log != null) {
            this.id = log.getId();
            this.createdAt = log.getCreatedAt();
            this.actorUserId = log.getActorUserId();
            this.actorEmail = log.getActorEmail();
            this.actorName = log.getActorName();
            this.actorRole = log.getActorRole();
            this.action = log.getAction();
            this.entityType = log.getEntityType();
            this.entityId = log.getEntityId();
            this.status = log.getStatus();
            this.ipAddress = log.getIpAddress();
            this.userAgent = log.getUserAgent();
            this.description = log.getDescription();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getActorRole() {
        return actorRole;
    }

    public void setActorRole(String actorRole) {
        this.actorRole = actorRole;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
